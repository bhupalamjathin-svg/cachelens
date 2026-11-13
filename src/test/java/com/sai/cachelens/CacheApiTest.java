package com.sai.cachelens;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the real application (capacity 2, LRU) and drives it over HTTP via MockMvc. */
@SpringBootTest(properties = {"cachelens.policy=LRU", "cachelens.capacity=2"})
@AutoConfigureMockMvc
class CacheApiTest {

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(delete("/api/cache")).andExpect(status().isNoContent());
    }

    private void putValue(String key, String value) throws Exception {
        mvc.perform(put("/api/cache/" + key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"" + value + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void storedValueIsReturnedOnHitAndMissGives404() throws Exception {
        putValue("user42", "Sai");
        mvc.perform(get("/api/cache/user42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hit").value(true))
                .andExpect(jsonPath("$.value").value("Sai"));
        mvc.perform(get("/api/cache/nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.hit").value(false));
    }

    @Test
    void thirdPutReportsTheEvictedKey() throws Exception {
        putValue("a", "1");
        putValue("b", "2");
        mvc.perform(put("/api/cache/c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"3\"}"))
                .andExpect(jsonPath("$.evicted").value("a"));
    }

    @Test
    void blankValueIsRejected() throws Exception {
        mvc.perform(put("/api/cache/x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statsAndHealthEndpointsRespond() throws Exception {
        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy").value("LRU"))
                .andExpect(jsonPath("$.capacity").value(2));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

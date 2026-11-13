package com.sai.cachelens;

import com.sai.cachelens.cache.LfuCache;
import com.sai.cachelens.cache.LruCache;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit tests for the two eviction algorithms — no Spring context, run in milliseconds. */
class EvictionPolicyTest {

    @Test
    void lruEvictsLeastRecentlyUsedKey() {
        LruCache c = new LruCache(2);
        c.put("a", "1");
        c.put("b", "2");
        c.get("a");                                   // a is now most recent, b is LRU
        assertEquals(Optional.of("b"), c.put("c", "3"));
        assertTrue(c.get("b").isEmpty());
        assertEquals(List.of("a", "c"), c.evictionOrder());
    }

    @Test
    void lfuEvictsLeastFrequentlyUsedKeyAndBreaksTiesByRecency() {
        LfuCache c = new LfuCache(2);
        c.put("a", "1");
        c.put("b", "2");
        c.get("a");
        c.get("a");                                   // a: freq 3, b: freq 1
        assertEquals(Optional.of("b"), c.put("c", "3"));

        c.put("d", "4");                              // c and d both freq 1 -> older one (c) goes
        assertTrue(c.get("c").isEmpty());
        assertEquals(List.of("d", "a"), c.evictionOrder());
    }

    @Test
    void updatingExistingKeyNeverEvicts() {
        LruCache c = new LruCache(1);
        c.put("a", "1");
        assertTrue(c.put("a", "2").isEmpty());
        assertEquals(Optional.of("2"), c.get("a"));
    }

    @Test
    void zeroCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LruCache(0));
        assertThrows(IllegalArgumentException.class, () -> new LfuCache(0));
    }
}

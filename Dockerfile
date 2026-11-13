# ---------- Stage 1: build ----------
# Full JDK + Maven, used only to compile and package. None of this ends up in the final image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# Download dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
ARG REVISION=1.0.0-SNAPSHOT
RUN mvn -B -q package -DskipTests -Drevision=${REVISION} \
 && cp target/cachelens-${REVISION}.jar /src/app.jar

# ---------- Stage 2: runtime ----------
# JRE only: no compiler, no Maven, no source code -> smaller image, smaller attack surface.
FROM eclipse-temurin:17-jre-jammy
ARG REVISION=1.0.0-SNAPSHOT
LABEL org.opencontainers.image.title="cachelens" \
      org.opencontainers.image.version="${REVISION}"

RUN useradd --system --uid 10001 cachelens
WORKDIR /app
COPY --from=build /src/app.jar /app/app.jar
USER 10001

ENV APP_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

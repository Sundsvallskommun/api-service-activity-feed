# Builds the service from source, so that Dokploy (and docker compose) can build straight from the repo.
# The tests run in CI (.github/workflows), not here.

# Stage 1: build with Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
# Dependencies in a separate layer, so they are only downloaded again when pom.xml changes.
RUN mvn -B -q dependency:go-offline -Dmaven.gitcommitid.skip=true || true
COPY src ./src
# git-commit-id needs .git, which is not part of the build context. CI checks the formatting.
RUN mvn -B -q package -DskipTests -Dmaven.gitcommitid.skip=true -Dspotless.check.skip=true \
 && cp target/api-service-activity-feed-*.jar /build/app.jar

# Stage 2: runtime image (same base as the dept44 services in Sundsvalls kommun)
FROM eclipse-temurin:25-jre
WORKDIR /app
# curl for the health check, so it does not depend on what the base image happens to contain.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -u 10001 -r -s /usr/sbin/nologin appuser
COPY --from=build /build/app.jar /app/app.jar
RUN mkdir -p /app/log && chown -R appuser:appuser /app
USER appuser

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

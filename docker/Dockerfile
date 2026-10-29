# Bygger tjänsten ur källkoden, så att Dokploy (och docker compose) kan bygga direkt från repot.
# Testerna körs i CI (.github/workflows), inte här.

# Steg 1: bygg med Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
# Beroenden i ett eget lager, så att de bara laddas ner igen när pom.xml ändras.
RUN mvn -B -q dependency:go-offline -Dmaven.gitcommitid.skip=true || true
COPY src ./src
# git-commit-id kräver .git (som inte skickas med i byggkontexten); formateringen kontrolleras i CI.
RUN mvn -B -q package -DskipTests -Dmaven.gitcommitid.skip=true -Dspotless.check.skip=true \
 && cp target/api-service-activity-feed-*.jar /build/app.jar

# Steg 2: körtidsbild (samma bas som dept44-tjänsterna i Sundsvalls kommun)
FROM eclipse-temurin:25-jre
WORKDIR /app
# curl för hälsokontrollen, så att den inte beror på vad basbilden råkar innehålla.
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

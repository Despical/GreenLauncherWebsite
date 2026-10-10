FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew
COPY src src
RUN ./gradlew --no-daemon --max-workers=2 bootJar

FROM eclipse-temurin:25-jre
LABEL org.opencontainers.image.title="Green Launcher Website" \
      org.opencontainers.image.source="https://github.com/Despical/GreenLauncherWebsite" \
      org.opencontainers.image.licenses="GPL-3.0-or-later"
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 greenweb \
    && useradd --system --uid 10001 --gid greenweb --home /app greenweb \
    && mkdir /app/data \
    && chown greenweb:greenweb /app/data
COPY --from=build /app/build/libs/green-launcher-web-1.0.0.jar app.jar
USER greenweb
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8087
VOLUME ["/app/data"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent http://localhost:8087/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

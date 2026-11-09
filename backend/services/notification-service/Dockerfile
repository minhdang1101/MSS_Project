# Build context: backend/ (the child pom needs the parent pom at ../../pom.xml)
#   docker build -f services/notification-service/Dockerfile -t dlms/notification-service backend
# Same layout as infrastructure/eureka-server/Dockerfile. "--mount=type=cache" keeps the Maven repository between
# builds (and between the images), so dependencies are downloaded once instead of once per image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/notification-service/pom.xml services/notification-service/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f services/notification-service/pom.xml dependency:go-offline
COPY services/notification-service/src services/notification-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f services/notification-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/notification-service/target/notification-service-*.jar app.jar
# application.properties is git-ignored and never copied in (backend/.dockerignore), so the non-secret
# settings live here.
# Mail server (Mailpit) and dispatch interval come from docker-compose.yml.
# Feign timeouts use the "default" key: an env var cannot keep the dash of "member-service".
ENV SPRING_APPLICATION_NAME=notification-service \
    SERVER_PORT=8085 \
    SPRING_JPA_HIBERNATE_DDLAUTO=update \
    SPRING_JPA_OPENINVIEW=false \
    EUREKA_INSTANCE_PREFERIPADDRESS=true \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info \
    SPRING_CLOUD_OPENFEIGN_CLIENT_CONFIG_DEFAULT_CONNECTTIMEOUT=2000 \
    SPRING_CLOUD_OPENFEIGN_CLIENT_CONFIG_DEFAULT_READTIMEOUT=3000 \
    APP_NOTIFICATION_RETRYDELAYMS=60000 \
    APP_NOTIFICATION_LISTENER_MAXATTEMPTS=3
RUN groupadd --system dlms && useradd --system --gid dlms dlms
USER dlms
EXPOSE 8085
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

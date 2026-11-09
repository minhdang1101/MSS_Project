# Build context: backend/ (the child pom needs the parent pom at ../../pom.xml)
#   docker build -f services/loan-service/Dockerfile -t dlms/loan-service backend
# Same layout as infrastructure/eureka-server/Dockerfile. "--mount=type=cache" keeps the Maven repository between
# builds (and between the images), so dependencies are downloaded once instead of once per image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/loan-service/pom.xml services/loan-service/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f services/loan-service/pom.xml dependency:go-offline
COPY services/loan-service/src services/loan-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f services/loan-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/loan-service/target/loan-service-*.jar app.jar
# application.properties is git-ignored and never copied in (backend/.dockerignore), so the non-secret
# settings live here.
# The due-date scan schedule comes from docker-compose.yml so a demo can run it often.
ENV SPRING_APPLICATION_NAME=loan-service \
    SERVER_PORT=8083 \
    SPRING_JPA_HIBERNATE_DDLAUTO=update \
    SPRING_JPA_OPENINVIEW=false \
    EUREKA_INSTANCE_PREFERIPADDRESS=true \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info \
    APP_LOAN_DEFAULTPERIODDAYS=14 \
    APP_LOAN_DUESOONDAYS=2 \
    APP_OUTBOX_RELAYINTERVALMS=5000
RUN groupadd --system dlms && useradd --system --gid dlms dlms
USER dlms
EXPOSE 8083
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

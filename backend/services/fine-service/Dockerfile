# Build context: backend/ (the child pom needs the parent pom at ../../pom.xml)
#   docker build -f services/fine-service/Dockerfile -t dlms/fine-service backend
# Same layout as infrastructure/eureka-server/Dockerfile. "--mount=type=cache" keeps the Maven repository between
# builds (and between the images), so dependencies are downloaded once instead of once per image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/fine-service/pom.xml services/fine-service/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f services/fine-service/pom.xml dependency:go-offline
COPY services/fine-service/src services/fine-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f services/fine-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/fine-service/target/fine-service-*.jar app.jar
# application.properties is git-ignored and never copied in (backend/.dockerignore), so the non-secret
# settings live here.
# Default fine policy values already have defaults in FinePolicyBootstrap.
ENV SPRING_APPLICATION_NAME=fine-service \
    SERVER_PORT=8084 \
    SPRING_JPA_HIBERNATE_DDLAUTO=update \
    SPRING_JPA_OPENINVIEW=false \
    EUREKA_INSTANCE_PREFERIPADDRESS=true \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
RUN groupadd --system dlms && useradd --system --gid dlms dlms
USER dlms
EXPOSE 8084
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Build context: backend/ (the child pom needs the parent pom at ../../pom.xml)
#   docker build -f infrastructure/api-gateway/Dockerfile -t dlms/api-gateway backend
# Same layout as infrastructure/eureka-server/Dockerfile. "--mount=type=cache" keeps the Maven repository between
# builds (and between the images), so dependencies are downloaded once instead of once per image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY infrastructure/api-gateway/pom.xml infrastructure/api-gateway/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f infrastructure/api-gateway/pom.xml dependency:go-offline
COPY infrastructure/api-gateway/src infrastructure/api-gateway/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f infrastructure/api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/infrastructure/api-gateway/target/api-gateway-*.jar app.jar
# application.properties is git-ignored and never copied in (backend/.dockerignore), so the non-secret
# settings live here.
# JWT secret and CORS origin come from docker-compose.yml.
ENV SPRING_APPLICATION_NAME=api-gateway \
    SERVER_PORT=8080 \
    EUREKA_INSTANCE_PREFERIPADDRESS=true \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,gateway
RUN groupadd --system dlms && useradd --system --gid dlms dlms
USER dlms
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

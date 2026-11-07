# Build context: backend/ (the child pom needs the parent pom at ../../pom.xml)
#   docker build -f infrastructure/eureka-server/Dockerfile -t dlms/eureka-server backend
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY infrastructure/eureka-server/pom.xml infrastructure/eureka-server/
# Dependencies in their own layer: a source change does not download them again
RUN mvn -B -q -f infrastructure/eureka-server/pom.xml dependency:go-offline
COPY infrastructure/eureka-server/src infrastructure/eureka-server/src
RUN mvn -B -q -f infrastructure/eureka-server/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/infrastructure/eureka-server/target/eureka-server-*.jar app.jar
# application.properties is git-ignored and never copied in, so the registry settings live here.
# Nothing in them is secret.
ENV SPRING_APPLICATION_NAME=eureka-server \
    SERVER_PORT=8761 \
    EUREKA_CLIENT_REGISTERWITHEUREKA=false \
    EUREKA_CLIENT_FETCHREGISTRY=false \
    EUREKA_SERVER_ENABLESELFPRESERVATION=false \
    EUREKA_SERVER_WAITTIMEINMSWHENSYNCEMPTY=0 \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
RUN groupadd --system dlms && useradd --system --gid dlms dlms
USER dlms
EXPOSE 8761
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD curl -fsS http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

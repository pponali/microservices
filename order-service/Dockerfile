# order-service — order placement, with idempotency + bulkhead/circuit-breaker
#
# Requires Redis at runtime (idempotency filter) — set REDIS_HOST / REDIS_PORT / REDIS_PASSWORD.
# Requires MySQL — set spring.datasource.url override or use the docker-compose.
#
# Build with:  docker build -f order-service/Dockerfile -t order-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl order-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/order-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]

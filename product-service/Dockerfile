# product-service — product catalogue with @Cacheable + Redis
#
# Note: server.port=0 → random port, registered with Eureka. EXPOSE is a placeholder.
# Requires MongoDB at runtime — override spring.data.mongodb.uri.
# Requires Redis at runtime — override spring.data.redis.host etc.
#
# Build with:  docker build -f product-service/Dockerfile -t product-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl product-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/product-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

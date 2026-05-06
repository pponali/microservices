# notification-service — Kafka consumer + outbound webhook fan-out
#
# Note: server.port=0 → random port, registered with Eureka. EXPOSE is a placeholder.
#
# Build with:  docker build -f notification-service/Dockerfile -t notification-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl notification-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/notification-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

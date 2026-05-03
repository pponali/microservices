# inventory-service
#
# Note: server.port=0 → JVM picks a random port and registers it with Eureka.
# EXPOSE 8080 is a placeholder; in real deployments map -p host:container or
# rely on the gateway to route via service discovery.
#
# Build with:  docker build -f inventory-service/Dockerfile -t inventory-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl inventory-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/inventory-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

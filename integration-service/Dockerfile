# integration-service — Kafka + Redis integration sandbox
#
# Build with:  docker build -f integration-service/Dockerfile -t integration-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl integration-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/integration-service/target/*.jar app.jar
EXPOSE 7070
ENTRYPOINT ["java","-jar","/app/app.jar"]

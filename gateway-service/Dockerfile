# gateway-service — Spring Cloud Gateway with rate-limiting + circuit breakers
#
# Build with:  docker build -f gateway-service/Dockerfile -t gateway-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl gateway-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/gateway-service/target/*.jar app.jar
EXPOSE 3333
ENTRYPOINT ["java","-jar","/app/app.jar"]

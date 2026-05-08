FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl catalog-service -am -DskipTests clean package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/catalog-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

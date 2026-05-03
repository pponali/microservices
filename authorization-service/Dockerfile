# authorization-service — OAuth 2.0 Authorization Server (Spring Authorization Server)
#
# Build context is the REPO ROOT (not this module dir) so the build sees the
# parent pom + sibling modules. Build with:
#
#   docker build -f authorization-service/Dockerfile -t authorization-service:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl authorization-service -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/authorization-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

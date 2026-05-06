# oauth2-client — sample OAuth2 client app (test driver for the auth flow)
#
# Build with:  docker build -f oauth2-client/Dockerfile -t oauth2-client:latest .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl oauth2-client -am -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/oauth2-client/target/*.jar app.jar
EXPOSE 5555
ENTRYPOINT ["java","-jar","/app/app.jar"]

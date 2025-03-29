FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/shipProxyClient.jar shipProxyClientDocker.jar
EXPOSE 8080
LABEL maintainer="ship-proxy-client"
ENTRYPOINT ["java", "-jar", "shipProxyClientDocker.jar"]
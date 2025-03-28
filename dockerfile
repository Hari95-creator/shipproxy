FROM openjdk:17-jdk-slim
LABEL maintainer="ship-Proxy-Client"
ADD target/shipProxy.jar shipProxyDocker.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "shipProxyDocker.jar"]
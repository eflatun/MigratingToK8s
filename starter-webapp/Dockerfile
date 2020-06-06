FROM openjdk:8-jdk-alpine
FROM maven:alpine
WORKDIR /app
COPY . /app
RUN mvn clean install -DskipTests
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} profiles.jar
ENTRYPOINT ["java","-jar","/profiles.jar"]
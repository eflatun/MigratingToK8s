FROM adoptopenjdk:11-jre-hotspot
RUN mkdir /opt/app
COPY ./target/profiles.jar /opt/app
ENTRYPOINT ["java","-jar","/opt/app/profiles.jar"]
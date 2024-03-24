FROM openjdk:17-jdk

ARG JAR_FILE=build/libs/once-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

COPY ./src/main/resources/crawling /crawling


ENTRYPOINT ["java","-jar","/app.jar"]

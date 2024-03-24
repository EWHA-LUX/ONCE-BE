FROM openjdk:17-jdk-slim-bullseye

RUN apt-get update && apt-get install -y python3 python3-pip


ARG JAR_FILE=build/libs/once-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime


COPY ./requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install -r requirements.txt


COPY ./src/main/resources/crawling /crawling


ENTRYPOINT ["java","-jar","/app.jar"]

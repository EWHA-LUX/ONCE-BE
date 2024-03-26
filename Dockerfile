FROM openjdk:17-jdk-slim-bullseye

RUN apt-get update && apt-get install -y python3 python3-pip

RUN apt -y install wget
RUN apt -y install unzip
RUN apt -y install curl

ARG JAR_FILE=build/libs/once-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb

RUN apt -y install ./google-chrome-stable_current_amd64.deb

RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/` curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE`/chromedriver_linux64.zip

RUN unzip /tmp/chromedriver.zip chromedriver -d /usr/bin

COPY ./requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install -r requirements.txt

COPY ./src/main/resources/crawling /crawling

ENTRYPOINT ["java","-jar","/app.jar"]
FROM openjdk:17-jdk-slim-bullseye

RUN apt-get update && apt-get install -y python3 python3-pip wget unzip curl && apt-get install -y systemd && apt-get install -y tzdata

RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

RUN wget https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_114.0.5735.198-1_amd64.deb && \
    apt -y install ./google-chrome-stable_114.0.5735.198-1_amd64.deb

RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip && \
     unzip /tmp/chromedriver.zip -d /usr/bin && \
     chmod +x /usr/bin/chromedriver

COPY ./requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install -r requirements.txt

COPY ./src/main/resources/crawling /crawling

ARG JAR_FILE=build/libs/once-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

ENV TZ=Asia/Seoul

ENTRYPOINT ["java","-jar","/app.jar"]
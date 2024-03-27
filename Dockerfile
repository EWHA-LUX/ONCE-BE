FROM openjdk:17-jdk-slim-bullseye

RUN apt-get update && apt-get install -y python3 python3-pip wget unzip curl



RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

RUN wget https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_114.0.5735.198-1_amd64.deb

RUN apt -y install ./google-chrome-stable_current_amd64.deb

RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip

RUN unzip /tmp/chromedriver.zip chromedriver -d /usr/bin

COPY ./requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install -r requirements.txt

COPY ./src/main/resources/crawling /crawling

ARG JAR_FILE=build/libs/once-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
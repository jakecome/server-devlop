FROM openjdk:8-jdk-alpine
MAINTAINER Jimmy.Liu <liuzm@glodon.com>
VOLUME ["/tmp", "/data"]
ADD pcop-weasvc-0.0.3.jar /app.jar
EXPOSE 7121
ENV PCOPOPTS=""
RUN echo "Asia/shanghai" > /etc/timezone
ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/./urandom $PCOPOPTS -jar /app.jar"]
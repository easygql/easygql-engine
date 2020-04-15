FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/*.jar app.jar
# 设置alpine和时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
EXPOSE 9995
FROM openjdk:21
# 设置时区为Asia/Shanghai
ENV TZ=Asia/Shanghai

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
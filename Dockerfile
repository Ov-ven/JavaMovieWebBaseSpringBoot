# 抛弃 docker.io，使用微软官方的 OpenJDK 17（国内免代理直连，速度极快）
FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

WORKDIR /app

# 设置时区为上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 直接将本地 IDEA 已经打好的 jar 包丢进镜像
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
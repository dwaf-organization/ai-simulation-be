# x86
FROM eclipse-temurin:21-jdk

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","app.jar"]

#FROM tomcat:10.1.30-jre21
# 톰캣 기본 webapps 폴더로 WAR 배포
#COPY build/libs/move-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
# 임시 디렉터리 권한 설정 및 ffmpeg 설치
#RUN chmod 1777 /tmp \
#	&& ulimit -n 65536 \
#    && apt update \
#    && apt install -y ffmpeg
#EXPOSE 8080
#CMD ["catalina.sh", "run"]
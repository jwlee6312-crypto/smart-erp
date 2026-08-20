# 1단계: 빌드 스테이지
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .

# 안드로이드 앱을 제외하고 백엔드 프로젝트만 빌드하도록 지정
RUN gradle :erp-backend:assemble -x test --no-daemon

# 빌드된 결과물을 실행 스테이지로 전달하기 위해 정리
RUN mkdir -p build/libs && \
    cp erp-backend/build/libs/*-SNAPSHOT.jar build/libs/app.jar

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/app.jar app.jar

# 서버 포트 및 시간대 설정
EXPOSE 8080
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 💡 자바 실행 옵션 최적화
ENTRYPOINT ["java", "-Xmx512m", "-Dspring.config.additional-location=file:/app/config/application.properties", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

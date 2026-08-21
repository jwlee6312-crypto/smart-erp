# 1. 빌드 스테이지 (JDK 21 적용)
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# 2. 실행 스테이지 (JRE 21 적용)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 서버 환경 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV STORAGE_PATH=/app/storage

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# 1. 빌드 스테이지
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# 2. 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 서버 환경 설정 (서버로 이전 시 이 값들이 우선 적용됨)
ENV SPRING_PROFILES_ACTIVE=prod
ENV STORAGE_PATH=/app/storage

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

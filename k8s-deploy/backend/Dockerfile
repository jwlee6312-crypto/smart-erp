FROM openjdk:21-jdk-slim

# 🚀 [정석] 이미지 빌드 시 음원 재생 필수 부품들을 미리 설치합니다.
# 이렇게 하면 실행 시마다 설치할 필요가 없어 부팅이 쾌속으로 진행됩니다.
RUN apt-get update && apt-get install -y \
    ffmpeg \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# AI 라이브러리도 미리 장착
RUN pip3 install --break-system-packages google-generativeai

WORKDIR /app
COPY erp-backend/build/libs/*.jar app.jar
COPY scripts/generate_tts.py /app/scripts/generate_tts.py

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

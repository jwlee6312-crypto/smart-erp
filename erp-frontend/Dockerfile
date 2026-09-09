# 1. 빌드 스테이지
FROM node:20-alpine AS build
WORKDIR /app

# 💡 [최적화] 의존성 설치 속도 개선 및 캐싱 극대화
COPY package*.json ./
RUN npm install --quiet --no-progress

# 소스 복사 및 빌드
COPY . .
RUN npm run build

# 2. 실행 스테이지 (Nginx)
FROM nginx:stable-alpine
# 빌드 결과물 복사 (캐시 효율을 위해 권한 및 압축 고려 가능)
COPY --from=build /app/dist /usr/share/nginx/html
# 🚀 커스텀 Nginx 설정 주입
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

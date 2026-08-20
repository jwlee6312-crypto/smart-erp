# 1. 빌드 스테이지
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2. 실행 스테이지 (Nginx)
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Nginx 설정 파일 (필요시 추가)
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

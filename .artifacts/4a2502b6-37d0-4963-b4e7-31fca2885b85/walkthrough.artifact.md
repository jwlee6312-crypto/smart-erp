# 네트워크 에러 해결 및 설정 최적화 완료 보고

로그인 시 발생하던 "Network Error" 문제를 해결하기 위해 Nginx 프록시 경로를 바로잡고, 문제 추적을 위한 로깅 설정을 최신화했습니다.

## 주요 변경 사항

### 1. 프론트엔드 Nginx 경로 수정 (`deployment.yaml`)
- **수정 전**: `proxy_pass http://erp-backend-service:8080/;` (끝에 `/`가 있어 `/api` 경로를 강제 삭제함)
- **수정 후**: `proxy_pass http://erp-backend-service:8080;` (원본 경로 그대로 백엔드에 전달)
- **효과**: 백엔드 컨트롤러가 `/api/comm/login` 요청을 정상적으로 인식할 수 있게 되어 404 및 네트워크 에러를 방지합니다.

### 2. 백엔드 로깅 패키지 경로 최신화 (`application-prod.properties`)
- **수정 전**: `logging.level.net.haion=DEBUG` (이전 패키지명)
- **수정 후**: `logging.level.com.crmbank.erp=DEBUG` (현재 패키지명)
- **효과**: 이제 백엔드 기동 및 에러 로그가 정상적으로 출력되어, 향후 문제 발생 시 정확한 원인 파악이 가능합니다.

## 향후 조치 권장 사항

> [!IMPORTANT]
> **설정 적용을 위해 아래 명령어를 순서대로 실행해 주세요:**
> 1.  **프론트엔드 재배포**: `kubectl apply -f k8s-deploy/frontend/deployment.yaml`
> 2.  **백엔드 재배포**: (필요 시) `kubectl apply -f k8s-deploy/backend/deployment.yaml`
> 3.  **브라우저 확인**: `https://smart-erp.duckdns.org` 에 다시 접속하여 로그인을 시도해 보세요.

이제 경로 문제가 해결되었으므로 "Network Error" 없이 정상적으로 API 통신이 이루어질 것입니다.

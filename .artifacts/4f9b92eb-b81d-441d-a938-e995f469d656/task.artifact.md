# ERP 데모 시스템 구축 작업 현황

- `[x]` 통합 `docker-compose.yml` 초안 작성
    - MSSQL, MySQL, Asterisk, Chatwoot, ERP-Backend, ERP-Frontend, Nginx Proxy Manager 연동
- `[x]` 보안 강화형 Asterisk 설정 파일 생성
    - `pjsip.conf`: Telnyx 연동 및 WebRTC 지원 설정
    - `extensions.conf`: 국내 전용(국제전화 차단) Dialplan 적용
- `[x]` 운영 환경용 Spring Boot 설정 (`application-prod.properties`) 작성
- `[x]` 시연용 가상 고객 데이터 생성 SQL 스크립트 작성
- `[x]` 삼성 공유기 포트 포워딩 및 가이드라인 정리
- `[x]` 신규 도메인(`smart-erp.duckdns.org`) NPM 등록 및 SSL 적용 완료
- `[x]` Vite 6 허용 호스트(allowedHosts) 신규 도메인으로 업데이트
- `[x]` 정석 보안 인프라(도메인 교체) 이관 완료
- `[ ]` 추가 서버(Ubuntu) 환경 설정 검증 (OS, DB, Asterisk, Chatwoot)

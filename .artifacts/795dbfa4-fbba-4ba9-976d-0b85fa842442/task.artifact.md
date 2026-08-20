# 모바일 백엔드 전체 미러링 작업 현황

- [/] `erp-mobile-backend` 초기화 및 웹 백엔드 소스 전체 복제
    - [ ] `erp-mobile-backend/src` 삭제
    - [ ] `erp-backend/src` -> `erp-mobile-backend/src` 복사
- [ ] 전체 패키지명 일괄 변경 (`com.crmbank.erp` -> `com.crmbank.erp.mobile`)
    - [ ] Java 소스 패키지 선언 및 임포트 수정
    - [ ] MyBatis XML 파일 내 클래스 경로 수정
- [ ] 모바일 전용 설정 적용
    - [ ] 서버 포트 8082 설정
    - [ ] 앱 이름 및 로깅 경로 수정
- [ ] 모바일 앱(`erp-mobile-app`) 접속 정보 업데이트
- [ ] 서버 기동 및 로그인 연동 검증

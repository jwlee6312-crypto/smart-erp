# 이미지 관리 체계 통합 및 연동 오류 최종 해결 완료 보고

이미지 관리 방식을 `storage` 하부의 용도별 폴더로 완전히 통합하고, 프론트엔드의 모든 호출 경로를 표준화하여 표시 오류를 완벽하게 해결했습니다.

## 주요 수정 사항

### 1. 저장 구조 통합 (D:/erp.crmbank.co.kr/storage/)
- **사용자 프로필**: `storage/profile/` 폴더에 통합 관리합니다.
- **회사 로고/직인**: `storage/{cmpycd}/{type}/` (logoimg, stampimg) 폴더에 회사별로 격리하여 관리합니다.

### 2. 백엔드 보정 (`FileUploadController.java`)
- 프로필 업로드 시 전용 폴더(`profile/`)를 사용하도록 수정하고, 프론트엔드 표준(소문자 키)에 맞춰 응답 필드명을 `filepath`로 통일했습니다.

### 3. 프론트엔드 연동 표준화
모든 이미지 호출 경로를 리소스 핸들러(`/Upload_Images/**`) 기반으로 단일화하고, 환경변수(`API_URL`)를 적용하여 절대 경로를 보장했습니다.
- **[SideMenu.vue](file:///D:/erp.crmbank.co.kr/erp-frontend/src/components/SideMenu.vue)**: 사이드바 사용자 사진 경로 수정
- **[HABA910U.vue](file:///D:/erp.crmbank.co.kr/erp-frontend/src/views/HABA/HABA910U.vue)**, **[HABA920U.vue](file:///D:/erp.crmbank.co.kr/erp-frontend/src/views/HABA/HABA920U.vue)**: 개인정보 및 사용자 관리 화면 사진 경로 수정
- **[HABA100U.vue](file:///D:/erp.crmbank.co.kr/erp-frontend/src/views/HABA/HABA100U.vue)**: 회사 로고 및 공인 직인 경로를 용도별 폴더 구조에 맞게 전면 수정

## 결과 확인 방법
1. **사용자 사진**: `HABA910U(개인정보 관리)`에서 사진을 업로드하면 사이드바와 본인 사진이 즉시 업데이트됩니다.
2. **회사 인장**: `HABA100U(환경설정)`에서 로고와 직인을 업로드하면 즉시 화면에 표시됩니다.
3. **저장 확인**: 업로드 후 반드시 상단의 **[저장]** 버튼을 눌러야 DB에 파일명이 기록되어 재접속 시에도 유지됩니다.

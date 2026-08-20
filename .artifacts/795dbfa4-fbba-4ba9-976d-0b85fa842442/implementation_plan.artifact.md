# 웹 백엔드 전체 미러링 및 모바일 백엔드 구축 계획

사용자님의 제안에 따라 `erp-backend`의 모든 로직과 설정을 `erp-mobile-backend`로 통째로 복사하여, 웹과 100% 동일한 로직을 가진 독립 모바일 서버를 신속하게 구축하는 계획입니다.

## User Review Required

> [!IMPORTANT]
> **패키지 자동 치환**
> - 모든 소스 코드의 패키지명을 `com.crmbank.erp`에서 **`com.crmbank.erp.mobile`**로 일괄 변경합니다.
> - 이를 통해 두 서버를 동시에 띄워도 클래스 충돌 없이 완벽하게 독립적으로 운영될 수 있습니다.

> [!NOTE]
> **독립 포트 운영**
> - 웹 백엔드: 8080 포트 유지
> - 모바일 백엔드: **8082 포트** 고정
> - 모바일 앱은 8082 포트를 바라보게 설정하여 웹 서버의 상태와 관계없이 동작하게 합니다.

## Proposed Changes

### 1. 백엔드 복제 및 동기화 (erp-mobile-backend)

#### [DELETE] 현재 모바일 백엔드 내용 초기화
- 기존의 불완전한 파일들을 모두 삭제하고 깨끗한 상태에서 시작합니다.

#### [COPY] 웹 백엔드 소스 전체 복사
- `erp-backend/src` 폴더의 모든 내용을 `erp-mobile-backend/src`로 복사합니다.

#### [REFACTOR] 일괄 패키지/경로 변경
- **Java**: `package com.crmbank.erp` -> `package com.crmbank.erp.mobile`
- **MyBatis XML**: 매퍼 namespace 및 resultType 경로 일괄 변경.
- **Config**: 서버 포트(8082) 및 앱 이름 수정.

### 2. 모바일 앱 설정

#### [MODIFY] [Config.java](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/java/com/crmbank/erp/mobile/Config.java)
- `SERVER_PORT`를 `8082`로 최종 확정합니다.

## Verification Plan

### Manual Verification
1. **서버 기동 확인**: `erp-mobile-backend` 폴더에서 `.\gradlew clean bootRun` 실행 후 로그에 에러가 없는지 확인.
2. **동기화 확인**: 웹 백엔드에서 수정된 최신 SQL 로직이 모바일 백엔드에서도 그대로 동작하는지 테스트.
3. **로그인 테스트**: 에뮬레이터 앱에서 8082 포트를 통해 실제 로그인이 성공하는지 확인.

### Automated Tests
- 전체 프로젝트 빌드를 통해 컴파일 및 설정 오류가 없는지 최종 검증합니다.

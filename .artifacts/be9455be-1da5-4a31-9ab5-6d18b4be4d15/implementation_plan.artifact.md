# 모바일 로그인 및 메뉴 기능 연동 계획

사용자님의 요청에 따라 모바일 에뮬레이터에서 로그인이 정상적으로 수행되고, 로그인 후 기존에 계획된 모바일 메뉴가 정상적으로 표시되도록 정비하겠습니다. 또한, 복사 과정에서 발생한 컴파일 에러의 원인을 설명드리고 코드의 가독성을 높이겠습니다.

## 사용자 리뷰 필요 사항

> [!IMPORTANT]
> **컴파일 에러 원인**: 이전에 발생한 에러들은 로직 오류가 아니라, 파일을 복사하는 과정에서 **파일 인코딩(EUC-KR vs UTF-8)**이 맞지 않아 한글 주석이 깨지고 따옴표가 어긋나서 발생한 것입니다. 현재 제가 모두 정비하여 **빌드 성공 상태**로 복구해 두었습니다.

> [!TIP]
> **에뮬레이터 접속**: 에뮬레이터에서 로컬 서버(Spring Boot)에 접속하려면 `erp-mobile-app`의 `Config.java`에 있는 `SERVER_IP`를 확인해야 합니다. 일반적으로 로컬 호스트 접속 시 `10.0.2.2`를 사용합니다.

## Proposed Changes

### [Component] 모바일 백엔드 (erp-mobile-backend)

#### [NEW] [MobileMenuController.java](file:///D:/erp.crmbank.co.kr/erp-mobile-backend/src/main/java/com/crmbank/erp/mobile/comm/controller/MobileMenuController.java)
- 모바일 앱(`MenuActivity.java`)이 기대하는 `/api/mobile/menus` 엔드포인트를 구현합니다.
- `PURCHASE`(입고), `SALES`(출고), `INVENTORY`(재고) 등 앱 로직에 정의된 메뉴 목록을 반환합니다.

#### [MODIFY] [CommController.java](file:///D:/erp.crmbank.co.kr/erp-mobile-backend/src/main/java/com/crmbank/erp/mobile/comm/controller/CommController.java)
- 깨진 한글 주석과 로그 메시지를 정상적인 한글로 복구하여 가독성을 높입니다.

### [Component] 모바일 앱 (erp-mobile-app)

#### [MODIFY] [Config.java](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/java/com/crmbank/erp/mobile/Config.java)
- 에뮬레이터 환경에 맞게 `SERVER_IP` 설정을 검토합니다. (사용자 지시에 따라 수정 가능)

## Verification Plan

### Automated Tests
- `gradlew :erp-mobile-backend:assemble`: 서버 빌드 성공 확인.

### Manual Verification
1. 서버 실행 후 에뮬레이터에서 로그인 시도.
2. 로그인 후 메인 메뉴 화면에 "입고관리", "출고관리" 등의 카드가 정상적으로 표시되는지 확인.

# [PLAN] 로컬 개발 환경 정비 및 모바일 백엔드 통합 계획

도커와 모바일 최적화로 인해 꼬였던 환경을 완전히 정리하고, 사용자님이 노트북에서 가장 효율적으로 작업하실 수 있도록 **메인 ERP와 모바일 백엔드(`erp-mobileback`)를 통합 관리**하는 환경을 구축합니다.

## 핵심 원칙
1.  **순수 로컬 지향**: 모든 작업은 노트북(Windows)의 안드로이드 스튜디오와 로컬 DB(SQL Server, MySQL)를 중심으로 진행합니다.
2.  **디자인 고수**: Git에 저장된 원본 디자인을 절대적으로 유지하며, 노트북 화면에서 투박하지 않게 원복된 상태를 보존합니다.
3.  **이중화 방지**: 모바일은 새로 만들지 않고, 기존에 잘 돌아가던 `erp-mobileback` 프로젝트를 수선하여 사용합니다.

## Proposed Changes

### [Development Environment]
#### [MODIFY] [settings.gradle](file:///D:/erp.crmbank.co.kr/settings.gradle)
- 현재 메인 ERP만 포함된 프로젝트 구성에 **`erp-mobileback` 모듈을 추가**합니다.
- 이제 안드로이드 스튜디오 하나에서 메인 백엔드와 모바일 백엔드를 동시에 수정하고 실행할 수 있습니다.

### [Mobile Backend]
#### [MODIFY] [env.properties](file:///D:/erp.crmbank.co.kr/erp-mobileback/src/main/resources/env.properties)
- 모바일 백엔드가 노트북의 로컬 DB에 정상적으로 붙을 수 있도록 접속 정보를 점검하고 보정합니다.
- `MOBILE_SERVER_PORT`를 `8082` 등으로 설정하여 메인 백엔드(`8080` 혹은 `8081`)와 충돌하지 않게 조절합니다.

### [Design Preservation]
- 최근에 제가 범했던 실수를 반복하지 않기 위해, `erp-frontend` 폴더 내의 CSS 및 레이아웃 파일은 **사용자님의 명시적 요청 없이는 절대 건드리지 않겠습니다.**

---

## User Review Required

> [!IMPORTANT]
> **모바일 백엔드 포트**: 메인 백엔드와 모바일 백엔드를 동시에 띄우려면 포트가 달라야 합니다. 메인이 `8080` 혹은 `8081`이라면, 모바일은 **`8082`**로 설정하는 것이 어떨까요?

---

## Verification Plan

### Manual Verification
1. 안드로이드 스튜디오 프로젝트 새로고침 후 `erp-mobileback` 모듈이 정상적으로 로드되는지 확인.
2. `erp-mobileback` 실행 시 로컬 DB 접속 및 API 응답 확인.
3. 메인 ERP 웹 화면의 디자인이 이전의 정교한 상태로 유지되는지 최종 확인.

---

**이 "로컬 중심 정비 및 모바일 통합" 계획을 진행할까요?**

승인해 주시면 복잡한 도커 대신 사용자님께 가장 익숙한 방식으로 개발 환경을 깔끔하게 세팅해 드리겠습니다.

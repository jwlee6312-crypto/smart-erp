# HSIO550U 주문출고처리 무결성 보장 및 백엔드 통합 저장 적용

`HSIO550U` 주문출고처리 시 마스터와 상세 내역이 별개의 API로 호출되어 트랜잭션 무결성이 깨지는 문제를 해결하기 위해, `HSOD100U` 패턴을 참조하여 백엔드 서비스 단에서 단일 트랜잭션으로 처리하는 기능을 구현합니다.

## User Review Required

> [!IMPORTANT]
> - 기존 프론트엔드에서의 개별 루프 저장 방식이 백엔드 단일 호출 방식으로 변경됩니다.
> - `HsioService`에 `@Transactional`을 적용하여 마스터 또는 상세 중 하나라도 실패하면 전체 롤백되도록 구성합니다.
> - `HSOD100U`의 표준 DTO 및 처리 로직을 그대로 이식하여 시스템 일관성을 유지합니다.

## Proposed Changes

### [Backend] DTO 및 서비스 로직 구현

#### [NEW] [Hsio550u.java](file:///D:/erp.crmbank.co.kr/erp-backend/src/main/java/com/crmbank/erp/hsio/dto/Hsio550u.java)
- `hsio_550u_str` 프로시저 파라미터 규격에 맞춘 마스터 DTO 생성

#### [NEW] [Hsio551u.java](file:///D:/erp.crmbank.co.kr/erp-backend/src/main/java/com/crmbank/erp/hsio/dto/Hsio551u.java)
- `hsio_551u_str` 프로시저 파라미터 규격에 맞춘 상세 DTO 생성

#### [NEW] [Hsio550uSaveRequest.java](file:///D:/erp.crmbank.co.kr/erp-backend/src/main/java/com/crmbank/erp/hsio/dto/Hsio550uSaveRequest.java)
- 마스터(`mst`)와 상세 리스트(`dtl`)를 포함하는 통합 요청 DTO 생성

#### [MODIFY] [HsioService.java](file:///D:/erp.crmbank.co.kr/erp-backend/src/main/java/com/crmbank/erp/hsio/service/HsioService.java)
- `saveOutbound550` 메소드 추가: `@Transactional` 적용 및 `HSOD100U` 방식의 키 채번/루프 저장 로직 구현

#### [MODIFY] [HsioController.java](file:///D:/erp.crmbank.co.kr/erp-backend/src/main/java/com/crmbank/erp/hsio/controller/HsioController.java)
- `/HSIO_550U_SAVE` 엔드포인트 추가 및 `executeProcedure` 스위치 케이스 연동

### [Frontend] API 연동 방식 변경

#### [MODIFY] [HSIO550U.vue](file:///D:/erp.crmbank.co.kr/erp-frontend/src/views/HSIO/HSIO550U.vue)
- `save` 함수 수정: 개별 API 호출 루프를 제거하고, 통합 DTO 구조로 단일 API(`saveOutbound550`)를 호출하도록 변경

## Verification Plan

### Automated Tests
- 없음 (기존 프로시저 로직 의존)

### Manual Verification
1. `HSIO550U` 화면에서 거래처 및 품목 선택 후 [저장] 클릭
2. 브라우저 개발자 도구(F12) Network 탭에서 `saveOutbound550` 호출 및 성공 응답 확인
3. DB에서 `HSIO230T_TBL`(마스터) 및 `HSIO240T_TBL`(상세) 데이터 생성 여부 확인
4. 강제로 상세 내역에 에러를 발생시켰을 때 마스터도 저장되지 않고 롤백되는지 확인 (트랜잭션 검증)

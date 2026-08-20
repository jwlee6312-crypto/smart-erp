# [HABA100U] 발주 승인 기능 화면 제거

사용자 요청에 따라 "발주 승인" 프로세스가 존재하지 않으므로, 환경설정 화면에서 해당 옵션을 제거합니다.

## User Review Required

> [!IMPORTANT]
> - **화면 제거**: `HABA100U.vue`에서 "발주승인 절차 사용" 체크박스를 제거합니다.
> - **데이터 모델 유지**: 백엔드와 약속된 22개 파라미터 구조를 깨뜨리지 않기 위해, `formData` 내의 `balcnfmyn` 값은 'N'으로 고정하여 내부적으로만 유지합니다. (백엔드 XML은 수정하지 않습니다.)

## Proposed Changes

### [Frontend] erp-frontend

#### [MODIFY] [HABA100U.vue](file:///D:/erp.crmbank.co.kr/erp-frontend/src/views/HABA/HABA100U.vue)
- "발주승인 절차 사용" 체크박스 UI 블록 삭제.
- `formData.balcnfmyn` 초기값을 'N'으로 명시.

## Verification Plan

### Manual Verification
- `HABA100U` 화면에서 "기타 옵션" 섹션에 "발주승인 절차 사용" 옵션이 보이지 않는지 확인.
- [저장] 시 기존의 22개 파라미터 규약에 따라 데이터가 정상적으로 전송되는지 확인.

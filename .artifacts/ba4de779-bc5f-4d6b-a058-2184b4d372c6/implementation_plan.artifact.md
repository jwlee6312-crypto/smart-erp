# 영업활동 관리 모바일 전환 개발 계획 (MHSAA100U/110U)

어제 개발된 웹의 영업활동 통합 관리(`HSAA100U`) 기능을 모바일 앱으로 전환합니다. `MHSIO052U`의 디자인 패턴과 백엔드 API 100% 활용 원칙을 준수합니다.

## User Review Required

> [!IMPORTANT]
> **디자인 패턴**: `MHSIO052U`와 동일하게 상단에 압축된 입력 폼, 하단에 품목 리스트를 배치합니다.
> **항목 구성**: 영업활동 항목이 많으므로 한 줄에 2~3개의 항목을 배치하는 컴팩트한 레이아웃을 적용합니다.
> **백엔드**: 기존 `/hsaa/...` API를 그대로 사용하며, 모바일 `ApiService`에 정의만 추가합니다.

## Proposed Changes

### [Network/API]

#### [MODIFY] [ApiService.java](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/java/com/crmbank/erp/mobile/ApiService.java)
- 영업활동 조회를 위한 `/hsaa/master`, `/hsaa/detail` 정의 추가
- 영업건 저장을 위한 `/hsaa/master/save` 정의 추가
- 공통코드 및 유저 조회를 위한 `/hsaa/codes/{group}`, `/hsaa/users` 추가

### [HSAA - 영업활동 리스트 (MHSAA100U)]

#### [NEW] [MHSAA100U.java](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/java/com/crmbank/erp/mobile/hsaa/MHSAA100U.java)
- 통합 영업 기회 리스트 조회 및 필터링 기능 구현
- 항목 클릭 시 상세(`MHSAA110U`)로 이동

#### [NEW] [activity_mhsaa100u.xml](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/res/layout/activity_mhsaa100u.xml)
- 상단: 등록기간(DateForm), 거래처/건명 검색, 담당자 선택 필터
- 중앙: 영업 기회 리스트 (`RecyclerView` 또는 `ListView`)

### [HSAA - 영업건 등록/상세 (MHSAA110U)]

#### [NEW] [MHSAA110U.java](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/java/com/crmbank/erp/mobile/hsaa/MHSAA110U.java)
- `MHSIO052U` 패턴을 계승한 등록/수정 로직
- **거래처 선택**: `PopupAdapter` 활용 (Gubun: `C4`)
- **품목 선택**: `PopupAdapter` 활용 (Gubun: `I1`)
- **공통코드 로드**: 진행상태, 중요도, 유치경로 등 코드 데이터 초기화

#### [NEW] [activity_mhsaa110u.xml](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/res/layout/activity_mhsaa110u.xml)
- `MHSIO052U` 스타일의 헤더 및 액션 버튼(신규, 저장, 삭제)
- **상단 폼**: 한 줄에 다수 항목 배치 (예: [영업담당/진행상태/중요도])
- **하단 리스트**: 영업건 관련 품목 리스트

#### [NEW] [item_mhsaa110u.xml](file:///D:/erp.crmbank.co.kr/erp-mobile-app/app/src/main/res/layout/item_mhsaa110u.xml)
- 품목 리스트 행 디자인 (품명, 수량, 단가, 금액)

## Verification Plan

### Automated Tests
- 없음 (UI 및 API 연동 테스트 중심)

### Manual Verification
1. `MHSAA100U` 접속 시 기존 영업건 리스트가 잘 나오는지 확인
2. 필터링(날짜, 담당자) 기능 정상 작동 확인
3. `MHSAA110U`에서 거래처 도움창으로 선택 시 정보 자동 매핑 확인
4. 품목 추가 후 저장 시 백엔드 DB에 정상 반영되는지 확인 (웹 HSAA100U와 데이터 동기화 확인)
5. 한 화면에 많은 항목이 가독성 있게 배치되었는지 확인

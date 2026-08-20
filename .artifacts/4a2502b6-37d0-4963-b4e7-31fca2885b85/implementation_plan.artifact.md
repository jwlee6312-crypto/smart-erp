# 설정 변수 최적화 및 불필요한 항목 제거 계획 (2차)

사용자의 확인에 따라 `MANUAL_PATH`를 포함하여 실제 사용되지 않는 항목들을 제거하고 설정을 최적화합니다.

## 분석 결과 및 유지/삭제 판단

### 유지 항목 (필수 보안/기능)
> [!IMPORTANT]
> 아래 항목은 시스템 전반의 핵심 기능(암호화, 업로드, 보안)을 담당하므로 유지합니다.
> - **`AESKEY`**: 이메일 및 Chatwoot 토큰 암복호화용.
> - **`MAX_FILE_SIZE` / `MAX_REQUEST_SIZE`**: 파일 업로드 제한용.
> - **`SECURITY_USERNAME` / `SECURITY_PASSWORD`**: 시스템 기본 관리자 계정.

### 삭제 항목 (잔재 및 불필요)
> [!TIP]
> 아래 항목은 현재 사용되지 않거나 DB로 대체된 항목들입니다.
> - **`MANUAL_PATH`**: 매뉴얼이 DB 기반으로 처리됨에 따라 파일 경로 설정은 더 이상 필요 없습니다.
> - **`BATCH_INITIALIZE_SCHEMA` / `BATCH_JOB_ENABLED`**: 배치 기능 미사용.
> - **`TIME_OUT`**: 다른 고정 설정(`PT10H`)에 의해 대체됨.
> - **`ALLOW_BEAN_DEFINITION_OVERRIDING`**: 참조하는 곳 없음.

## Proposed Changes

### 1. 로컬 환경 설정 정리 (`env.properties`)
- `MANUAL_PATH`, `BATCH_*`, `ALLOW_BEAN_DEFINITION_OVERRIDING`, `TIME_OUT` 제거.

### 2. 운영 환경 설정 정리 (`env-prod.properties`)
- 위와 동일하게 불필요 항목 제거.

### 3. 프로젝트 속성 파일 수정 (`application-prod.properties` 및 `ManualController.java`)
- `application-prod.properties`에서 배치 관련 설정 삭제.
- `ManualController.java`에서 `MANUAL_PATH` 참조 시 에러를 방지하기 위해 기본값(Empty)을 주거나 로직을 정리합니다.

## Verification Plan

### Automated Tests
- `grep`을 통해 제거된 키워드가 설정 파일에서 사라졌는지 확인합니다.

### Manual Verification
- 백엔드 기동 시 `MANUAL_PATH` 누락으로 인한 빈(Bean) 생성 오류가 없는지 확인합니다.

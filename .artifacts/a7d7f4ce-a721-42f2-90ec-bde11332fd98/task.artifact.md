# 🛠️ 서버 설치 및 통합 이전 작업 현황

- `[x]` **Phase 1: Ubuntu Server 24.04 OS 설치 (SSD)**
    - `[x]` 설치 미디어 부팅 및 초기 설정 완료
    - `[x]` 스토리지 구성 및 설치 완료
- `[/]` **Phase 2: 서버 기초 환경 구축**
    - `[x]` 스마트폰 USB 테더링 인터넷 연결 성공
    - `[/]` 서버 IP 주소 확인 및 SSH 접속 시도
    - `[ ]` 무선 랜카드(TP-Link/Realtek) 드라이버 설치
    - `[ ]` 도커(Docker) 및 필수 도구 설치
    - `[ ]` ZFS 데이터셋 최적화 (/data/mssql, /data/mysql 등)
- `[ ]` **Phase 3: 소스 및 데이터 통합 이전**
    - `[ ]` 프로젝트 소스 전체 전송 (/data/erp.crmbank.co.kr)
    - `[ ]` DB 백업 파일 복구 (MSSQL .bak / MySQL .sql)
    - `[ ]` 최신 SQL 변경분 일괄 반영
- `[ ]` **Phase 4: 시스템 가동 및 최종 검증**
    - `[ ]` Docker Compose 실행 및 컨테이너 상태 확인
    - `[ ]` 외부(웹/앱) 접속 및 통신 테스트

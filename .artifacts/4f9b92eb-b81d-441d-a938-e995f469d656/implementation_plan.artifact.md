# 정석 보안 환경(SSL/HTTPS) 도메인 교체 및 클린 설치 계획

기존 도메인(`crmbank.duckdns.org`)의 보안 이슈를 완전히 걷어내고, 새로운 도메인(`smart-erp.duckdns.org`)으로 깨끗하게 다시 시작하기 위한 정비 계획입니다.

## 1. 정비 전략: "비우고 새로 채우기"
기존의 꼬인 설정을 완전히 삭제하고, 가장 안전한 순서로 신규 도메인을 등록합니다.

## 2. 세부 조치 사항

### [1단계] Nginx Proxy Manager 클린 정리 (사용자 직접 수행)
1.  **NPM 관리 페이지(`localhost:81`)** 접속.
2.  **기존 도메인 삭제:** 목록에서 `crmbank.duckdns.org` 항목 우측의 점 3개 클릭 -> **[Delete]**를 선택하여 완전히 지웁니다.
3.  **새 도메인 등록 (인증서 제외):**
    - [Add Proxy Host] 클릭.
    - Details 탭: `Domain Names`에 **`smart-erp.duckdns.org`** 입력.
    - Forward IP: **`host.docker.internal`**, Port: **`5173`**.
    - **SSL 탭: `None`으로 설정** (중요: 등록부터 성공시키기 위함).
    - [Save] 클릭.

### [2단계] 보안 인증서(SSL) 입히기
1.  등록된 `smart-erp` 항목의 **[Edit]** 클릭.
2.  SSL 탭 이동 -> `Request a new SSL Certificate` 선택 -> 동의 체크 후 **[Save]**.

### [3단계] 시스템 설정 업데이트 (AI 수행)
- `vite.config.ts`의 허용 호스트(allowedHosts)를 `smart-erp.duckdns.org`로 변경.
- 백엔드 보안 설정 점검.

## 3. 검증 절차
1.  핸드폰 브라우저 캐시 삭제.
2.  `https://smart-erp.duckdns.org` 접속 및 로그인 시도.

---

## 사용자 확인 사항

> [!TIP]
> **기존 도메인 삭제 여부:** 네, 꼬임을 방지하기 위해 **기존 것은 깨끗이 지우시는 것이 좋습니다.**

위 계획대로 "기존 도메인 삭제 및 신규 도메인 클린 등록"을 진행할까요? 승인해 주시면 바로 코드 수정을 대기하겠습니다.

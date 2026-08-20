---

## 4. Asterisk PJSIP 보안 설정 (`pjsip.conf`)
Telnyx 연동 및 안드로이드 브라우저(WebRTC) 지원을 위한 핵심 설정입니다.

```ini
[transport-udp]
type=transport
protocol=udp
bind=0.0.0.0

[transport-wss]
type=transport
protocol=wss
bind=0.0.0.0

; Telnyx 연동 설정 (Outbound)
[telnyx]
type=registration
outbound_auth=telnyx_auth
server_uri=sip:sip.telnyx.com
client_uri=sip:YOUR_TELNYX_USER@sip.telnyx.com

[telnyx_auth]
type=auth
auth_type=password
password=YOUR_TELNYX_PASSWORD
username=YOUR_TELNYX_USER

; 안드로이드 웹폰(WebRTC) 엔드포인트
[webrtc_client]
type=endpoint
context=from-internal
disallow=all
allow=ulaw,alaw,vp8,h264
auth=webrtc_auth
aors=webrtc_client
dtls_auto_generate_cert=yes
webrtc=yes
use_avpf=yes
media_encryption=dtls
dtls_verify=fingerprint
dtls_setup=actpass
ice_support=yes

[webrtc_auth]
type=auth
auth_type=password
password=StrongWebRTCPassword!
username=demo_user

[webrtc_client]
type=aor
max_contacts=5
```

---

## 5. 운영 환경용 Spring Boot 설정 (`application-prod.properties`)
우분투 도커 환경에서 동작하도록 경로와 DB 주소를 최적화했습니다.

```properties
# 서버 포트 및 프로필
server.port=8080
spring.profiles.active=prod

# 데이터베이스 연동 (Docker 컨테이너 이름 사용)
spring.datasource.erp.url=jdbc:sqlserver://erp-db:1433;databaseName=erp_db;encrypt=true;trustServerCertificate=true
spring.datasource.erp.username=sa
spring.datasource.erp.password=YourStrongPassword123!

spring.datasource.asterisk.url=jdbc:mysql://asterisk-db:3306/asteriskdb?serverTimezone=Asia/Seoul
spring.datasource.asterisk.username=root
spring.datasource.asterisk.password=asterisk_pass

# 파일 저장 경로 (Docker Volume 매핑 경로)
MANUAL_PATH=/app/manual
STORAGE_PATH=/app/storage

# Asterisk CTI 연동 (Docker 내부 통신)
asterisk.host=asterisk
asterisk.port=5038
asterisk.username=admin
asterisk.password=gkdldhs12#$
```

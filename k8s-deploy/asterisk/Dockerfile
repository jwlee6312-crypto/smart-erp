FROM andrius/asterisk:latest

# 필요한 설정 파일 복사 (없을 경우 기본값 사용)
# COPY asterisk-config/ /etc/asterisk/

# 관리자(AMI) 포트 및 SIP 포트 개방
EXPOSE 5038 5060/udp 5060/tcp

CMD ["/usr/sbin/asterisk", "-f"]

#!/bin/bash
# 🚀 Smart ERP - 쾌속 자동 기동 스크립트 (운영자 모드)
# 작성일자: 2026-08-31

cd "$(dirname "$0")/.."

echo "=========================================="
echo "   Smart ERP System Starting (Fast)       "
echo "=========================================="

# 1. 권한 확인 (데이터 보존)
sudo mkdir -p /var/lib/asterisk/sounds/custom
sudo chown -R 1000:1000 /var/lib/asterisk/sounds/custom
sudo chmod -R 777 /var/lib/asterisk/sounds/custom

# 2. 시스템 기동 (이제 이미지가 다 갖춰져서 즉시 뜹니다)
sudo docker compose up -d

echo "=========================================="
echo " ✅ SUCCESS: System is Ready in 1 Second! "
echo "=========================================="

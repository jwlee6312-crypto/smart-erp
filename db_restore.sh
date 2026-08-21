#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (UTF-8 인코딩 보완판)
# 작성일자: 2026-08-21

# 💡 [주의] 이 스크립트는 서버의 .env 설정과 동일한 비밀번호를 사용해야 합니다.
# 아래 변수들을 서버 환경에 맞게 실제 값으로 수정 후 사용하세요.
MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System"
echo "========================================================"

echo "1. 백엔드 서버 일시 중단 (DB 커넥션 해제)..."
sudo docker-compose stop smart-erp-backend

echo "--------------------------------------------------------"
echo "2. MySQL (Asterisk) 복구 중 (utf8mb4 강제 지정)..."
# 💡 이전에 발생했던 한글 깨짐 문제를 원천 차단하기 위해 인코딩을 명시적으로 고정합니다.
mysql -u root -p"$MYSQL_PWD" --default-character-set=utf8mb4 asterisk < ./backups/asterisk_full.sql

if [ $? -eq 0 ]; then
    echo "✅ MySQL (Asterisk) 복구 완료!"
else
    echo "❌ MySQL 복구 실패 (인코딩 또는 권한 확인 요망)"
fi

echo "--------------------------------------------------------"
echo "3. MSSQL (SMARTDB) 복구 중..."
# 💡 DB 점유 문제를 방지하기 위해 SINGLE_USER 모드로 전환 후 덮어씌웁니다.
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PWD" -Q "
ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
RESTORE DATABASE [SMARTDB] FROM DISK = '/home/smart/smart-erp/backups/smartdb_full.bak' WITH REPLACE;
ALTER DATABASE [SMARTDB] SET MULTI_USER;"

if [ $? -eq 0 ]; then
    echo "✅ MSSQL (SMARTDB) 복구 완료!"
else
    echo "❌ MSSQL 복구 실패 (파일 경로 /home/smart/smart-erp/backups/ 를 확인하세요)"
fi

echo "--------------------------------------------------------"
echo "4. 백엔드 서버 재가동 및 로그 확인..."
sudo docker-compose start smart-erp-backend
sleep 2
sudo docker logs --tail 20 smart-erp-backend

echo "========================================================"
echo "🎉 모든 데이터 복구 프로세스가 종료되었습니다!"
echo "========================================================"

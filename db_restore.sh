#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (최종 보완판)
# 파일명 불일치 및 도구 경로 자동 탐색 로직 추가

MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System"
echo "========================================================"

# 1. 백엔드 서버 중단
echo "1. 백엔드 서버 일시 중단..."
sudo docker-compose stop smart-erp-backend

# 2. MySQL (Asterisk) 복구
echo "--------------------------------------------------------"
echo "2. MySQL (Asterisk) 복구 중..."
# 💡 사용자님이 알려주신 중복 확장자(.sql.sql)를 체크하여 유연하게 처리합니다.
MYSQL_FILE="./backups/asterisk_full.sql.sql"
if [ ! -f "$MYSQL_FILE" ]; then MYSQL_FILE="./backups/asterisk_full.sql"; fi

if [ -f "$MYSQL_FILE" ]; then
    mysql -u root -p"$MYSQL_PWD" --default-character-set=utf8mb4 asterisk < "$MYSQL_FILE"
    echo "✅ MySQL (Asterisk) 복구 완료! (사용한 파일: $MYSQL_FILE)"
else
    echo "❌ MySQL 백업 파일을 찾을 수 없습니다. (경로: ./backups/ )"
fi

# 3. MSSQL (SMARTDB) 복구
echo "--------------------------------------------------------"
echo "3. MSSQL (SMARTDB) 복구 중..."
# 💡 sqlcmd 경로를 자동으로 찾습니다.
SQLCMD_PATH=$(which sqlcmd)
if [ -z "$SQLCMD_PATH" ]; then SQLCMD_PATH="/opt/mssql-tools/bin/sqlcmd"; fi

if [ ! -z "$SQLCMD_PATH" ] && [ -x "$SQLCMD_PATH" ]; then
    sudo $SQLCMD_PATH -S localhost -U sa -P "$MSSQL_PWD" -Q "
    ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    RESTORE DATABASE [SMARTDB] FROM DISK = '$(pwd)/backups/smartdb_full.bak' WITH REPLACE;
    ALTER DATABASE [SMARTDB] SET MULTI_USER;"
    echo "✅ MSSQL (SMARTDB) 복구 완료!"
else
    echo "⚠️ 서버에 sqlcmd 도구가 없습니다. 노트북의 SSMS에서 직접 복구를 권장합니다."
fi

# 4. 서버 재가동
echo "--------------------------------------------------------"
echo "4. 백엔드 서버 재가동..."
sudo docker-compose start smart-erp-backend
sleep 2
sudo docker logs --tail 20 smart-erp-backend

echo "========================================================"
echo "🎉 작업 종료"
echo "========================================================"

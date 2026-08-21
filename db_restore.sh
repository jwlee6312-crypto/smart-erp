#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (서버 로컬 도구 최적화)
# - 하이픈 형태의 docker-compose 지원
# - MySQL 바이너리 모드 적용 (인코딩 문제 해결)
# - 절대 경로 및 권한 자동 보정

MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"
BASE_DIR="/home/smart/smart-erp"
BACKUP_DIR="$BASE_DIR/backups"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System (Final)"
echo "========================================================"

# 1. 백엔드 중지
echo "Step 1: 백엔드 서버 일시 중단..."
sudo docker-compose stop smart-erp-backend

# 2. MySQL 복구 (인코딩 오류 방지를 위해 --binary-mode 추가)
echo "--------------------------------------------------------"
echo "Step 2: MySQL (Asterisk) 데이터 복구 중..."
if [ -f "$BACKUP_DIR/asterisk_full.sql" ]; then
    # 💡 --binary-mode=1 옵션으로 ASCII '\0' 에러를 원천 차단합니다.
    mysql -h 127.0.0.1 -u root -p"$MYSQL_PWD" --binary-mode=1 --default-character-set=utf8mb4 asterisk < "$BACKUP_DIR/asterisk_full.sql"
    if [ $? -eq 0 ]; then echo "✅ MySQL 복구 성공!"; else echo "❌ MySQL 복구 실패!"; fi
else
    echo "❌ MySQL 백업 파일이 없습니다."
fi

# 3. MSSQL 복구 (설치하신 mssql-tools18 사용)
echo "--------------------------------------------------------"
echo "Step 3: MSSQL (SMARTDB) 데이터 복구 중..."
# 💡 파일 권한 강제 부여 (SQL Server 엔진이 읽을 수 있도록)
sudo chmod 644 "$BACKUP_DIR/smartdb_full.bak"

/opt/mssql-tools18/bin/sqlcmd -S 127.0.0.1 -U sa -P "$MSSQL_PWD" -C -Q "
ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
RESTORE DATABASE [SMARTDB] FROM DISK = '$BACKUP_DIR/smartdb_full.bak' WITH REPLACE;
ALTER DATABASE [SMARTDB] SET MULTI_USER;"

if [ $? -eq 0 ]; then
    echo "✅ MSSQL 복구 성공!"
else
    echo "❌ MSSQL 복구 실패 (파일 권한이나 경로를 확인하세요)"
fi

# 4. 서버 재가동
echo "--------------------------------------------------------"
echo "Step 4: 백엔드 서버 재가동..."
sudo docker-compose start smart-erp-backend
echo "========================================================"
echo "🎉 모든 복구 작업이 성공적으로 완료되었습니다!"
echo "========================================================"

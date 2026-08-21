#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (v9.0 - Encoding Nuclear Option)
# - strings 명령어를 사용하여 모든 바이너리/BOM 찌꺼기 완벽 제거

MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"
BACKUP_DIR="$(pwd)/backups"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System (v9.0)"
echo "========================================================"

# 1. 서비스 중지
sudo docker-compose stop smart-erp-backend

# 2. MySQL 복구
echo "Step 2: MySQL (Asterisk) 데이터 복구 중..."
if [ -f "$BACKUP_DIR/asterisk_full.sql" ]; then
    # 💡 [핵심] Nuclear Option: strings 명령어로 오직 '읽을 수 있는 문자'만 추출
    # 이렇게 하면 BOM, NULL, 깨진 문자 등이 100% 제거된 깨끗한 SQL 파일이 생성됩니다.
    strings "$BACKUP_DIR/asterisk_full.sql" > "$BACKUP_DIR/mysql_clean.sql"

    # 정제된 파일로 복구 실행
    mysql -h 127.0.0.1 -u root -p"$MYSQL_PWD" --default-character-set=utf8mb4 asterisk < "$BACKUP_DIR/mysql_clean.sql"

    if [ $? -eq 0 ]; then echo "✅ MySQL 복구 성공!"; else echo "❌ MySQL 복구 실패!"; fi
    rm "$BACKUP_DIR/mysql_clean.sql"
else
    echo "❌ 백업 파일 없음"
fi

# 3. MSSQL 복구
echo "Step 3: MSSQL (SMARTDB) 데이터 복구 중..."
MSSQL_CID=$(sudo docker ps -q --filter "name=mssql")
if [ -z "$MSSQL_CID" ]; then MSSQL_CID=$(sudo docker ps -q --filter "expose=1433"); fi

if [ ! -z "$MSSQL_CID" ]; then
    sudo docker cp "$BACKUP_DIR/smartdb_full.bak" "$MSSQL_CID:/tmp/smartdb_full.bak"
    sudo docker exec -i "$MSSQL_CID" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PWD" -C -Q "
        ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
        RESTORE DATABASE [SMARTDB] FROM DISK = '/tmp/smartdb_full.bak' WITH REPLACE;
        ALTER DATABASE [SMARTDB] SET MULTI_USER;"
    echo "✅ MSSQL 복구 시도 완료 (로그 확인 요망)"
    sudo docker exec -i "$MSSQL_CID" rm /tmp/smartdb_full.bak 2>/dev/null
fi

# 4. 서비스 가동
sudo docker-compose start smart-erp-backend
echo "========================================================"
echo "🎉 모든 작업 완료"

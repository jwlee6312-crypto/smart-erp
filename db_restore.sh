#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (컨테이너 & 인코딩 대응판)
# 작성일자: 2026-08-21

MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"
BASE_DIR="/home/smart/smart-erp"
BACKUP_DIR="$BASE_DIR/backups"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System (v4.0)"
echo "========================================================"

# 1. 백엔드 중지
echo "Step 1: 백엔드 서버 일시 중단..."
sudo docker-compose stop smart-erp-backend

# 2. MySQL (Asterisk) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 2: MySQL (Asterisk) 데이터 복구 중..."
if [ -f "$BACKUP_DIR/asterisk_full.sql" ]; then
    # 💡 Windows 인코딩(UTF-16)을 리눅스 표준(UTF-8)으로 변환하여 임시 파일 생성
    iconv -f UTF-16 -t UTF-8 "$BACKUP_DIR/asterisk_full.sql" > "$BACKUP_DIR/asterisk_ready.sql" 2>/dev/null || cp "$BACKUP_DIR/asterisk_full.sql" "$BACKUP_DIR/asterisk_ready.sql"

    mysql -h 127.0.0.1 -u root -p"$MYSQL_PWD" --default-character-set=utf8mb4 asterisk < "$BACKUP_DIR/asterisk_ready.sql"
    if [ $? -eq 0 ]; then echo "✅ MySQL 복구 성공!"; else echo "❌ MySQL 복구 실패!"; fi
    rm "$BACKUP_DIR/asterisk_ready.sql"
else
    echo "❌ MySQL 백업 파일이 없습니다."
fi

# 3. MSSQL (SMARTDB) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 3: MSSQL (SMARTDB) 데이터 복구 중..."
# 💡 현재 실행 중인 MSSQL 컨테이너 ID를 자동으로 찾습니다.
MSSQL_CONTAINER=$(sudo docker ps -q --filter "ancestor=mcr.microsoft.com/mssql/server")
if [ -z "$MSSQL_CONTAINER" ]; then
    MSSQL_CONTAINER=$(sudo docker ps -q | xargs -I {} sudo docker inspect {} --format '{{.Name}}' | grep mssql | head -1 | tr -d '/')
fi

if [ ! -z "$MSSQL_CONTAINER" ]; then
    echo "📦 감지된 MSSQL 컨테이너: $MSSQL_CONTAINER"
    # 💡 백업 파일을 컨테이너 내부로 복사 (경로 문제 해결의 핵심)
    sudo docker cp "$BACKUP_DIR/smartdb_full.bak" "$MSSQL_CONTAINER:/tmp/smartdb_full.bak"

    /opt/mssql-tools18/bin/sqlcmd -S 127.0.0.1 -U sa -P "$MSSQL_PWD" -C -Q "
    ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    RESTORE DATABASE [SMARTDB] FROM DISK = '/tmp/smartdb_full.bak' WITH REPLACE;
    ALTER DATABASE [SMARTDB] SET MULTI_USER;"

    if [ $? -eq 0 ]; then echo "✅ MSSQL 복구 성공!"; else echo "❌ MSSQL 복구 실패!"; fi
    # 임시 파일 삭제
    sudo docker exec "$MSSQL_CONTAINER" rm /tmp/smartdb_full.bak
else
    echo "⚠️ MSSQL 컨테이너를 찾을 수 없습니다. 직접 복구를 시도합니다..."
    /opt/mssql-tools18/bin/sqlcmd -S 127.0.0.1 -U sa -P "$MSSQL_PWD" -C -Q "
    ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    RESTORE DATABASE [SMARTDB] FROM DISK = '$BACKUP_DIR/smartdb_full.bak' WITH REPLACE;
    ALTER DATABASE [SMARTDB] SET MULTI_USER;"
fi

# 4. 서버 재가동
echo "--------------------------------------------------------"
echo "Step 4: 백엔드 서버 재가동..."
sudo docker-compose start smart-erp-backend

echo "========================================================"
echo "🎉 모든 복구 작업이 완료되었습니다!"
echo "========================================================"

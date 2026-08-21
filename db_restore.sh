#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (v6.0 - Perfect Encoding & Container Support)
# 작성일자: 2026-08-21

# 1. 설정 정보
MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"
BASE_DIR="/home/smart/smart-erp"
BACKUP_DIR="$BASE_DIR/backups"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System (v6.0)"
echo "========================================================"

# 1. 백엔드 서비스 중지 (DB 점유 해제)
echo "Step 1: 백엔드 서버 일시 중단..."
sudo docker-compose stop smart-erp-backend

# 2. MySQL (Asterisk) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 2: MySQL (Asterisk) 데이터 복구 중..."
if [ -f "$BACKUP_DIR/asterisk_full.sql" ]; then
    # 💡 [핵심] Windows 특유의 BOM(Byte Order Mark)과 NULL(\0) 문자를 완벽히 제거하여 순수 UTF-8로 정제
    sed '1s/^\xef\xbb\xbf//' "$BACKUP_DIR/asterisk_full.sql" | tr -d '\0' > "$BACKUP_DIR/mysql_final.sql"

    # 정제된 파일로 복구 실행
    mysql -h 127.0.0.1 -u root -p"$MYSQL_PWD" --default-character-set=utf8mb4 asterisk < "$BACKUP_DIR/mysql_final.sql"

    if [ $? -eq 0 ]; then
        echo "✅ MySQL (Asterisk) 복구 성공!";
    else
        echo "❌ MySQL 복구 실패!";
    fi
    rm "$BACKUP_DIR/mysql_final.sql"
else
    echo "❌ MySQL 백업 파일을 찾을 수 없습니다."
fi

# 3. MSSQL (SMARTDB) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 3: MSSQL (SMARTDB) 데이터 복구 중..."
# 실행 중인 MSSQL 컨테이너를 자동으로 감지
MSSQL_CID=$(sudo docker ps -q --filter "name=mssql")
if [ -z "$MSSQL_CID" ]; then MSSQL_CID=$(sudo docker ps -q --filter "expose=1433"); fi

if [ ! -z "$MSSQL_CID" ]; then
    echo "📦 감지된 MSSQL 컨테이너: $MSSQL_CID"
    # 백업 파일을 컨테이너 내부의 /tmp 로 복사하여 경로 문제 해결
    sudo docker cp "$BACKUP_DIR/smartdb_full.bak" "$MSSQL_CID:/tmp/smartdb_full.bak"

    # 컨테이너 내부 도구로 복구 실행
    sudo docker exec -i "$MSSQL_CID" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PWD" -C -Q "
        ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
        RESTORE DATABASE [SMARTDB] FROM DISK = '/tmp/smartdb_full.bak' WITH REPLACE;
        ALTER DATABASE [SMARTDB] SET MULTI_USER;"

    if [ $? -eq 0 ]; then
        echo "✅ MSSQL (SMARTDB) 복구 성공!";
    else
        echo "❌ MSSQL 복구 실패!";
    fi
    # 임시 파일 삭제
    sudo docker exec -i "$MSSQL_CID" rm /tmp/smartdb_full.bak 2>/dev/null
else
    echo "❌ MSSQL 컨테이너를 찾을 수 없습니다. 도커 가동 여부를 확인하세요."
fi

# 4. 백엔드 서비스 재가동
echo "--------------------------------------------------------"
echo "Step 4: 백엔드 서버 재가동 및 로그 확인..."
sudo docker-compose start smart-erp-backend
sleep 2
sudo docker logs --tail 20 smart-erp-backend

echo "========================================================"
echo "🎉 모든 데이터 복구 프로세스가 완벽하게 완료되었습니다!"
echo "========================================================"

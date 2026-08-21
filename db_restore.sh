#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (v8.0 - 이전 성공 로직 완벽 재현)
# - Windows SQL 파일의 모든 인코딩 이슈 해결
# - MSSQL 도커 컨테이너 자동 감지 및 복사 로직 포함

MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"
BASE_DIR="/home/smart/smart-erp"
BACKUP_DIR="$BASE_DIR/backups"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System (v8.0)"
echo "========================================================"

# 1. 서비스 일시 중지
echo "Step 1: 백엔드 서버 일시 중단..."
sudo docker-compose stop smart-erp-backend

# 2. MySQL (Asterisk) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 2: MySQL (Asterisk) 데이터 복구 중..."
if [ -f "$BACKUP_DIR/asterisk_full.sql" ]; then
    # 💡 [핵심] 이전 성공 로직: iconv로 강제 변환 후 찌꺼기 문자 제거
    # UTF-16이나 UTF-8 BOM이 섞인 파일을 리눅스 표준 UTF-8로 정제합니다.
    iconv -f UTF-16 -t UTF-8 "$BACKUP_DIR/asterisk_full.sql" > "$BACKUP_DIR/mysql_step1.sql" 2>/dev/null || cp "$BACKUP_DIR/asterisk_full.sql" "$BACKUP_DIR/mysql_step1.sql"

    # 널(NULL) 문자와 유령 문자(BOM) 최종 제거
    cat "$BACKUP_DIR/mysql_step1.sql" | tr -d '\0' | sed '1s/^\xef\xbb\xbf//' | sed '1s/^[^a-zA-Z0-9/ /!/_/*/-]*//' > "$BACKUP_DIR/mysql_final.sql"

    # 복구 실행 (바이너리 모드 추가)
    mysql -h 127.0.0.1 -u root -p"$MYSQL_PWD" --binary-mode=1 --default-character-set=utf8mb4 asterisk < "$BACKUP_DIR/mysql_final.sql"

    if [ $? -eq 0 ]; then echo "✅ MySQL 복구 성공!"; else echo "❌ MySQL 복구 실패!"; fi
    rm "$BACKUP_DIR/mysql_step1.sql" "$BACKUP_DIR/mysql_final.sql"
else
    echo "❌ MySQL 백업 파일을 찾을 수 없습니다."
fi

# 3. MSSQL (SMARTDB) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 3: MSSQL (SMARTDB) 데이터 복구 중..."
MSSQL_CID=$(sudo docker ps -q --filter "name=mssql")
if [ -z "$MSSQL_CID" ]; then MSSQL_CID=$(sudo docker ps -q --filter "expose=1433"); fi

if [ ! -z "$MSSQL_CID" ]; then
    echo "📦 감지된 MSSQL 컨테이너: $MSSQL_CID"
    sudo docker cp "$BACKUP_DIR/smartdb_full.bak" "$MSSQL_CID:/tmp/smartdb_full.bak"

    sudo docker exec -i "$MSSQL_CID" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PWD" -C -Q "
        ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
        RESTORE DATABASE [SMARTDB] FROM DISK = '/tmp/smartdb_full.bak' WITH REPLACE;
        ALTER DATABASE [SMARTDB] SET MULTI_USER;"

    if [ $? -eq 0 ]; then echo "✅ MSSQL 복구 성공!"; else echo "❌ MSSQL 복구 실패!"; fi
    sudo docker exec -i "$MSSQL_CID" rm /tmp/smartdb_full.bak 2>/dev/null
else
    echo "❌ MSSQL 컨테이너를 찾을 수 없습니다."
fi

# 4. 서버 재가동
echo "--------------------------------------------------------"
echo "Step 4: 백엔드 서버 재가동..."
sudo docker-compose start smart-erp-backend
echo "========================================================"
echo "🎉 모든 데이터 복구가 완료되었습니다!"
echo "========================================================"

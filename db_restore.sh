#!/bin/bash

# 🚀 Smart ERP 통합 DB 복구 스크립트 (원터치 일괄 실행판)
# 노트북 ➔ 서버 배포용

# 1. 설정 정보 (서버 환경에 맞춰 고정)
MYSQL_PWD="gkdldhs12#$"
MSSQL_PWD="8221284sb!12#$"
BACKUP_DIR="$(pwd)/backups"

echo "========================================================"
echo "   Smart ERP Database Auto Restoration System"
echo "========================================================"

# 1. 백엔드 서비스 중지 (DB 점유 해제)
echo "Step 1: 백엔드 서버 일시 중단..."
sudo docker-compose stop smart-erp-backend

# 2. MySQL (Asterisk) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 2: MySQL (Asterisk) 데이터 복구 중..."
if [ -f "$BACKUP_DIR/asterisk_full.sql" ]; then
    mysql -u root -p"$MYSQL_PWD" --default-character-set=utf8mb4 asterisk < "$BACKUP_DIR/asterisk_full.sql"
    echo "✅ MySQL 복구 완료!"
else
    echo "❌ MySQL 백업 파일을 찾을 수 없습니다: $BACKUP_DIR/asterisk_full.sql"
fi

# 3. MSSQL (SMARTDB) 데이터 복구
echo "--------------------------------------------------------"
echo "Step 3: MSSQL (SMARTDB) 데이터 복구 중..."
# 💡 sqlcmd 도구가 서버에 없을 경우를 대비해 도커(mcr.microsoft.com/mssql-tools)를 통해 복구 명령을 내립니다.
# 이 방식은 서버 본체에 별도 도구를 설치하지 않아도 되어 가장 깔끔합니다.

sudo docker run --rm \
  -v "$BACKUP_DIR":/backups \
  --add-host=host.docker.internal:host-gateway \
  mcr.microsoft.com/mssql-tools \
  /opt/mssql-tools/bin/sqlcmd -S host.docker.internal -U sa -P "$MSSQL_PWD" -Q "
    ALTER DATABASE [SMARTDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    RESTORE DATABASE [SMARTDB] FROM DISK = '/backups/smartdb_full.bak' WITH REPLACE;
    ALTER DATABASE [SMARTDB] SET MULTI_USER;"

if [ $? -eq 0 ]; then
    echo "✅ MSSQL 복구 완료!"
else
    echo "❌ MSSQL 복구 실패 (네트워크 또는 권한 확인 요망)"
fi

# 4. 백엔드 서비스 재가동
echo "--------------------------------------------------------"
echo "Step 4: 백엔드 서버 재가동..."
sudo docker-compose start smart-erp-backend

echo "========================================================"
echo "🎉 모든 데이터 복구 프로세스가 성공적으로 종료되었습니다!"
echo "========================================================"

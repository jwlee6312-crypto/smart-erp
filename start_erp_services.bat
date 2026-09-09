@echo off
TITLE Smart ERP - 서비스 자동 정상화 도구
SETLOCAL EnableDelayedExpansion

:: 🚀 [권한 체크] 관리자 권한으로 자동 실행
openfiles >nul 2>&1
if %errorlevel% neq 0 (
    powershell Start-Process -FilePath '%0' -Verb RunAs
    exit /b
)

echo ======================================================
echo [1/4] 우분투(WSL) IP 주소 자동 추적 중...
echo ======================================================
for /f "tokens=1" %%i in ('wsl -d Ubuntu hostname -I') do set WSL_IP=%%i
echo 🎯 확인된 우분투 IP: %WSL_IP%

echo.
echo ======================================================
echo [2/4] 윈도우-우분투 포트 배달 통로(netsh) 갱신 중...
echo ======================================================
netsh interface portproxy delete v4tov4 listenport=5060 listenaddress=0.0.0.0 >nul 2>&1
netsh interface portproxy add v4tov4 listenport=5060 listenaddress=0.0.0.0 connectport=5060 connectaddress=%WSL_IP%
echo ✅ 5060번 포트 연결 완료!

echo.
echo ======================================================
echo [3/4] Asterisk 22 교환기 엔진 재시작 중...
echo ======================================================
:: 🚀 설정 파일을 건드리지 않고 서비스만 재시작합니다.
wsl -d Ubuntu -u root service asterisk restart
echo ✅ 아스테리스크 엔진이 깨어났습니다.

echo.
echo ======================================================
echo [4/4] Chatwoot 데몬(Rails/Sidekiq) 가동 중...
echo ======================================================
:: 사용자님의 기억대로 데몬 실행 명령만 전송합니다.
wsl -d Ubuntu -u root sh -c "nohup bundle exec rails s -b 0.0.0.0 > /dev/null 2>&1 &"
wsl -d Ubuntu -u root sh -c "nohup bundle exec sidekiq > /dev/null 2>&1 &"
echo ✅ 채팅 엔진 소생 완료.

echo.
echo ======================================================
echo 모든 시스템이 정상화되었습니다! 이제 ERP를 사용하세요.
echo ======================================================
pause

@echo off
echo ===========================================
echo   🚀 GitHub Auto Push + Release Script
echo ===========================================
echo.

:: 현재 디렉토리 표시
echo [작업 디렉토리]
cd
echo.

:: main 브랜치로 전환
echo [main 브랜치로 이동 중...]
git switch main
echo.

:: 원격 최신 코드 가져오기
echo [최신 상태로 pull 중...]
git pull origin main
echo.

:: 변경사항 추가 및 커밋
echo [변경사항 확인 중...]
git status
echo.
set /p commitmsg=커밋 메시지를 입력하세요 (Enter만 누르면 자동 메시지): 

if "%commitmsg%"=="" (
  set commitmsg=Auto commit from Windows batch script
)

git add .
git commit -m "%commitmsg%"
echo.

:: main 브랜치 push
echo [main 브랜치 push 중...]
git push origin main
echo.

:: 태그 push
echo [모든 태그 push 중...]
git push origin --tags
echo.

:: Release 자동 생성
echo [GitHub CLI로 Releases 자동 생성 중...]
for /f "tokens=*" %%t in ('git tag') do (
    echo ▶ %%t Release 생성 중...
    gh release create "%%t" --title "%%t" --notes "Auto release for tag %%t" >nul 2>&1
)
echo.

echo ===========================================
echo ✅ 모든 변경사항, 태그, 릴리스가 GitHub에 반영되었습니다!
echo ===========================================
pause

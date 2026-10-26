@echo off
setlocal
cd /d "%~dp0android"
where gradle >nul 2>nul
if errorlevel 1 (
 echo ไม่พบ Gradle. เปิดโฟลเดอร์ android ด้วย Android Studio หรือใช้ GitHub Actions ตาม README_TH.txt
 pause
 exit /b 1
)
call gradle --no-daemon :app:assembleDebug
if errorlevel 1 (
 echo Build failed. Check error messages above.
 pause
 exit /b 1
)
echo APK: %CD%\app\build\outputs\apk\debug\app-debug.apk
pause

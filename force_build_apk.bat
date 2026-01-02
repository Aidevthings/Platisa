@echo off
echo ========================================
echo FORCE BUILD APK - Platisa
echo ========================================
echo.
echo Cleaning build...
call gradlew.bat clean
echo.
echo Building APK with force rerun...
call gradlew.bat assembleDebug --rerun-tasks
echo.
echo ========================================
echo Build complete! Check app\build\outputs\apk\debug\
echo ========================================
pause

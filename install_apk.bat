@echo off
echo ========================================
echo INSTALL APK ON PHONE - Platisa
echo ========================================
echo.
echo Make sure your phone is connected via USB!
echo.
adb devices
echo.
echo Uninstalling old version...
adb uninstall com.example.platisa
echo.
echo Installing new APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.
echo ========================================
echo Installation complete!
echo ========================================
pause

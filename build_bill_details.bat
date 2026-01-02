@echo off
echo Building Platisa with new Bill Details screen...
cd "A:\Software Dev\Platisa"
call gradlew.bat assembleDebug --stacktrace > build_bill_details.txt 2>&1
echo Build complete! Check build_bill_details.txt for results.

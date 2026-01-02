# Platisa Cleanup & Optimization Report

## Files to DELETE (Safe to Remove)

### Build Logs & Error Files (Root Directory)
```
assemble_output.txt
assembly_error.txt
build_error.log
build_error.txt through build_error8.txt
build_errors.txt
build_error_log.txt, build_error_log_2.txt
build_fix.txt
build_log.txt through build_log_7.txt
build_log_*.txt (all variations)
build_out2.txt
build_output.txt
build_output_txt
build_pdf417.txt
build_qr_flow.txt
clean_build.txt
compile_error.txt
compile_output.txt
debug_compile.txt
err2.txt through err14.txt
err_full.txt
error.log
gradle_tasks_error.txt
gradle_tasks_output.txt
gradle_tasks_retry.txt, gradle_tasks_retry2.txt
latest_compile.txt
p1_compile_error.txt
settings_errors.txt
test_compile_log.txt
test_log.txt, test_log_2.txt
test_output.txt
```

### Redundant Documentation (Keep only essential)
```
# KEEP:
- PLATISA.md (main documentation)
- SERBIAN_LANGUAGE_GUIDE.md (important for parsing)

# DELETE (outdated/redundant):
- BILL_CARD_CLICK_FIX.md
- BILL_DETAILS_IMPLEMENTATION.md
- BILL_DETAILS_NAVIGATION_SETUP.md
- BILL_DUPLICATE_DETECTION_IMPLEMENTATION.md
- BORDER_SIMPLIFICATION_UPDATE.md
- BORDER_SIMPLIFICATION_VISUAL_REFERENCE.md
- DIAGNOSTIC_FIX_GUIDE.md
- DOCUMENTATION_INDEX.md
- DUPLICATE_CHECK_FIX.md
- DUPLICATE_CHECK_IMPLEMENTATION_GUIDE.md
- DYNAMIC_FONT_SIZING_IMPLEMENTATION.md
- DYNAMIC_SIZE_TEXT_QUICK_REFERENCE.md
- DYNAMIC_SIZE_TEXT_USAGE_GUIDE.md
- DYNAMIC_SIZING_VISUAL_FLOW.md
- FIX_INSTRUCTIONS.md
- FIX_SUMMARY.md
- HOME_PAGE_REDESIGN_COMPLETE_SUMMARY.md
- HOME_PAGE_REDESIGN_SUMMARY.md
- IMPLEMENTATION_CHECKLIST.md
- NOTIFICATION_SYSTEM.md
- SUMMARY_GRID_EXACT.txt
- platisa_implementation_plan.md
- Sta da se radi.txt
- fix_script.txt
- QR kod resenje za citanje.txt
```

### Miscellaneous Files
```
build_bill_details.bat (if not used)
build_camera.txt
build_camera2.txt
```

### Source Code Files
```
app/src/main/java/.../BillDetailsScreen_BACKUP.kt (backup file - delete)
```

## Code Optimizations Applied

### 1. Theme.kt
- Added `background` property to PlatisaCustomColors for test support

### 2. Formatters.kt  
- Added `formatDate()` method for consistent date formatting

## Recommended Code Optimizations

### 1. Reduce Debug Logging in Production
In `EpsParser.kt`, wrap debug logs:
```kotlin
private const val DEBUG = BuildConfig.DEBUG

private fun log(message: String) {
    if (DEBUG) android.util.Log.d("EpsParser", message)
}
```

### 2. HomeViewModel Optimization
- Currency conversion rate should be a constant
- Consider caching the receipts filter result

### 3. BillDuplicateDetector
- The normalize functions are called multiple times - consider caching

## Commands to Run Cleanup

### PowerShell (Windows)
```powershell
cd "A:\Software Dev\Platisa"

# Delete all .txt files (except important ones)
Get-ChildItem -Filter "*.txt" | Where-Object { 
    $_.Name -notmatch "local.properties|gradle" 
} | Remove-Item -Force

# Delete build logs
Remove-Item "build_*.txt" -Force -ErrorAction SilentlyContinue
Remove-Item "err*.txt" -Force -ErrorAction SilentlyContinue
Remove-Item "*.log" -Force -ErrorAction SilentlyContinue

# Delete old MD docs (be selective)
# Review before deleting!
```

### Gradle Clean
```bash
./gradlew clean
```

## Directory Structure After Cleanup

```
Platisa/
├── app/
│   └── src/
│       ├── main/
│       ├── test/
│       └── androidTest/
├── gradle/
├── Primeri/              # Keep - test bills
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties
├── gradle.properties
├── gradlew / gradlew.bat
├── firebase.json
├── firestore.*
├── PLATISA.md
├── SERBIAN_LANGUAGE_GUIDE.md
└── .gitignore
```

## Estimated Space Savings
- ~50+ unnecessary text files
- ~20+ outdated documentation files
- Build cache cleanup with `./gradlew clean`

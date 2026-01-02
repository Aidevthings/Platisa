# Platisa Project Cleanup Script
# Run from: A:\Software Dev\Platisa
# Usage: .\cleanup.ps1

Write-Host "=== PLATISA PROJECT CLEANUP ===" -ForegroundColor Cyan
Write-Host ""

# Confirm before proceeding
$confirm = Read-Host "This will delete temporary files. Continue? (y/n)"
if ($confirm -ne 'y') {
    Write-Host "Cancelled." -ForegroundColor Yellow
    exit
}

$deletedCount = 0

# 1. Delete build log files
Write-Host "Deleting build logs..." -ForegroundColor Yellow
$buildLogs = @(
    "assemble_output.txt",
    "assembly_error.txt",
    "build_error*.txt",
    "build_errors.txt",
    "build_error_log*.txt",
    "build_fix.txt",
    "build_log*.txt",
    "build_out*.txt",
    "build_output*",
    "build_pdf417.txt",
    "build_qr_flow.txt",
    "build_camera*.txt",
    "clean_build.txt",
    "compile_error.txt",
    "compile_output.txt",
    "debug_compile.txt",
    "latest_compile.txt",
    "p1_compile_error.txt",
    "test_compile_log.txt",
    "test_log*.txt",
    "test_output.txt"
)

foreach ($pattern in $buildLogs) {
    $files = Get-ChildItem -Path . -Filter $pattern -ErrorAction SilentlyContinue
    foreach ($file in $files) {
        Remove-Item $file.FullName -Force -ErrorAction SilentlyContinue
        $deletedCount++
        Write-Host "  Deleted: $($file.Name)" -ForegroundColor DarkGray
    }
}

# 2. Delete error files
Write-Host "Deleting error files..." -ForegroundColor Yellow
$errorFiles = Get-ChildItem -Path . -Filter "err*.txt" -ErrorAction SilentlyContinue
foreach ($file in $errorFiles) {
    Remove-Item $file.FullName -Force -ErrorAction SilentlyContinue
    $deletedCount++
    Write-Host "  Deleted: $($file.Name)" -ForegroundColor DarkGray
}

# 3. Delete log files
Write-Host "Deleting log files..." -ForegroundColor Yellow
$logFiles = Get-ChildItem -Path . -Filter "*.log" -ErrorAction SilentlyContinue
foreach ($file in $logFiles) {
    Remove-Item $file.FullName -Force -ErrorAction SilentlyContinue
    $deletedCount++
    Write-Host "  Deleted: $($file.Name)" -ForegroundColor DarkGray
}

# 4. Delete gradle task outputs
Write-Host "Deleting gradle task outputs..." -ForegroundColor Yellow
$gradleFiles = @("gradle_tasks_error.txt", "gradle_tasks_output.txt", "gradle_tasks_retry.txt", "gradle_tasks_retry2.txt", "settings_errors.txt")
foreach ($file in $gradleFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force -ErrorAction SilentlyContinue
        $deletedCount++
        Write-Host "  Deleted: $file" -ForegroundColor DarkGray
    }
}

# 5. Delete misc temporary files
Write-Host "Deleting misc temporary files..." -ForegroundColor Yellow
$miscFiles = @("fix_script.txt", "Sta da se radi.txt", "QR kod resenje za citanje.txt", "SUMMARY_GRID_EXACT.txt")
foreach ($file in $miscFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force -ErrorAction SilentlyContinue
        $deletedCount++
        Write-Host "  Deleted: $file" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "=== CLEANUP COMPLETE ===" -ForegroundColor Green
Write-Host "Deleted $deletedCount files" -ForegroundColor Green
Write-Host ""

# Optional: Gradle clean
$gradleClean = Read-Host "Run 'gradlew clean' to clear build cache? (y/n)"
if ($gradleClean -eq 'y') {
    Write-Host "Running gradlew clean..." -ForegroundColor Yellow
    & .\gradlew.bat clean
}

Write-Host ""
Write-Host "Done! Review CLEANUP_REPORT.md for additional manual cleanup options." -ForegroundColor Cyan

@echo off
setlocal enabledelayedexpansion

REM Runs unit + integration tests for all services that have tests.
REM Note: Docker Desktop must be running (integration tests start Kafka/Redis/MySQL).

set SERVICES=processing-service monitoring-service alert-service ingest-service device-registry-service
set FAILED=

for %%s in (%SERVICES%) do (
    echo.
    echo ============================================================
    echo   Testing: %%s
    echo ============================================================
    pushd %%s
    call gradlew.bat test --console=plain
    if errorlevel 1 (
        set FAILED=!FAILED! %%s
    )
    popd
)

echo.
echo ============================================================
if "!FAILED!"=="" (
    echo   RESULT: ALL TESTS PASSED ^(green^)
) else (
    echo   RESULT: FAILED SERVICES:!FAILED!
)
echo ============================================================
echo.
pause

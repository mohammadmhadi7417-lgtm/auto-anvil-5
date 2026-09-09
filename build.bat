@echo off
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo Java 21 is not installed or is not on PATH.
  echo Install JDK 21 and run this file again.
  pause
  exit /b 1
)
if not exist gradlew.bat (
  echo No Gradle Wrapper is present. Use GitHub Actions workflow for online build.
  pause
  exit /b 1
)
call gradlew.bat build --no-daemon
if errorlevel 1 exit /b %errorlevel%
echo.
echo Build complete. Check build\libs\
pause

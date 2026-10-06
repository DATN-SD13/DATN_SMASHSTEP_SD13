@echo off
setlocal
cd /d "%~dp0"
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul
if not errorlevel 1 (
    echo Port 8080 is already in use. Stop the backend before running a clean build.
    exit /b 1
)
call mvnw.cmd clean compile
if errorlevel 1 exit /b 1
call mvnw.cmd spring-boot:run
exit /b %ERRORLEVEL%

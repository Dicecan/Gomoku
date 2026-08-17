@echo off
setlocal
cd /d "%~dp0"

if exist "%ProgramFiles%\Microsoft\jdk-17.0.10.7-hotspot" (
    set "JAVA_HOME=%ProgramFiles%\Microsoft\jdk-17.0.10.7-hotspot"
)

call gradlew.bat jar
pause

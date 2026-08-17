@echo off
setlocal
cd /d "%~dp0"

if exist "%ProgramFiles%\Microsoft\jdk-17.0.10.7-hotspot\bin\java.exe" (
    set "JAVA_EXE=%ProgramFiles%\Microsoft\jdk-17.0.10.7-hotspot\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

set "JAR_FILE=build\libs\Gomoku-1.0.0.jar"

if not exist "%JAR_FILE%" (
    echo Building application...
    call gradlew.bat jar
)

echo Starting Nazuna Gomoku...
"%JAVA_EXE%" -Xms64m -Xmx256m -jar "%JAR_FILE%"
pause

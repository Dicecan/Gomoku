@echo off
title NAZUNA GOMOKU PRO
color 0B
setlocal
cd /d "%~dp0"

if exist "%ProgramFiles%\Microsoft\jdk-17.0.10.7-hotspot\bin\java.exe" (
    set "JAVA_EXE=%ProgramFiles%\Microsoft\jdk-17.0.10.7-hotspot\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

set "JAR_FILE=build\libs\Gomoku-1.0.0.jar"

if not exist "%JAR_FILE%" (
    echo [*] Packaging application...
    call gradlew.bat jar
)

"%JAVA_EXE%" -Xms64m -Xmx256m -Dfile.encoding=UTF-8 -jar "%JAR_FILE%"
pause

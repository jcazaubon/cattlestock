@echo off
rem Windows: compila el nucleo del dominio (Java puro, sin framework) y corre sus pruebas.
rem Usa el JDK de JAVA_HOME si esta disponible; si no, el javac del PATH.
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "OUT=%TEMP%\cattlestock-out"
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%" >nul
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
)
set "FILES="
for /R "src\main\java\com\kinetix\cattlestock\domain" %%f in (*.java) do set "FILES=!FILES! "%%f""
for /R "src\test\java\com\kinetix\cattlestock\domain" %%f in (*.java) do set "FILES=!FILES! "%%f""
"%JAVAC%" -d "%OUT%" %FILES%
if errorlevel 1 exit /b 1
"%JAVA%" -cp "%OUT%" com.kinetix.cattlestock.domain.LedgerTest
exit /b %errorlevel%

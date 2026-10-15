@echo off
rem Atajo al check del proyecto: todo corre con Maven.
cd /d "%~dp0"
call mvnw.cmd -ntp test

@echo off
call mvn -f "%~dp0pom.xml" dependency:go-offline
exit /b %ERRORLEVEL%

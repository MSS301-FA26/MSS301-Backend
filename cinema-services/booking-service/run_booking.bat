@echo off
setlocal
cd /d "%~dp0"
call ..\mvnw.cmd clean spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=target"
exit /b %errorlevel%

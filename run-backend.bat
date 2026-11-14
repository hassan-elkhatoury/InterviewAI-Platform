@echo off
echo Starting InterviewAI backend...

:: Load environment variables from .env if present
if exist "%~dp0.env" (
    for /f "usebackq eol=# tokens=1* delims==" %%A in ("%~dp0.env") do (
        set "%%A=%%B"
    )
)

:: Default fallbacks if not defined in .env
if not defined DB_URL set DB_URL=jdbc:mysql://localhost:3306/interviewai?connectionTimeZone=UTC
if not defined DB_USERNAME set DB_USERNAME=interviewai
if not defined DB_PASSWORD set DB_PASSWORD=interviewai
if not defined JWT_SECRET set JWT_SECRET=4b8e2f89c0a1e3d5b7a9c2e4f6a8b0c2d4e6f8a0b2c4d6e8f0a2b4c6d8e0f2a4

java -jar "%~dp0interviewai-backend\target\interviewai-backend-0.2.0-SNAPSHOT.jar"

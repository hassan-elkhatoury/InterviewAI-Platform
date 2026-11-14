@echo off
echo Stopping InterviewAI backend on port 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do (
    taskkill /F /PID %%a 2>nul
)
echo Done.


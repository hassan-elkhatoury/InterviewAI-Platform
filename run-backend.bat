@echo off
echo Starting InterviewAI backend...
set DB_URL=jdbc:mysql://localhost:3306/interviewai?connectionTimeZone=UTC
set DB_USERNAME=interviewai
set DB_PASSWORD=interviewai
set JWT_SECRET=4b8e2f89c0a1e3d5b7a9c2e4f6a8b0c2d4e6f8a0b2c4d6e8f0a2b4c6d8e0f2a4
java -jar interviewai-backend/target/interviewai-backend-0.2.0-SNAPSHOT.jar

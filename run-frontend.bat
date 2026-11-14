@echo off
echo Starting InterviewAI desktop client...
set INTERVIEWAI_API_URL=http://localhost:8080/api/v1
set MAVEN_OPTS=--enable-native-access=ALL-UNNAMED
mvn -pl interviewai-frontend javafx:run
pause

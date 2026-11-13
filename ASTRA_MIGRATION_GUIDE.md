# Specification: InterviewAI (Spring Boot 3 + React) - Simple & Explainable Architecture

## 1. Goal & Guiding Principle
Refactor `C:\Users\Hassan\Desktop\InterviewAI` into a clean, modern full-stack web app.
**Rule**: Keep code simple, readable, and free of over-engineering so it can be explained clearly in any job interview.

---

## 2. Project Layout
```text
InterviewAI/
├── backend/                   # Spring Boot 3 (Port 8080)
│   ├── pom.xml
│   └── src/main/java/com/interviewai/
│       ├── controller/        # Simple @RestController endpoints
│       ├── dto/               # Clean Java record DTOs
│       ├── entity/            # Minimal JPA @Entity classes
│       ├── repository/        # Simple JpaRepository interfaces
│       ├── service/           # Business logic, PDFBox parser & OpenRouter client
│       └── BackendApp.java    # Spring Boot entry point
└── frontend/                  # React + Vite + Tailwind (Port 3000)
    ├── package.json
    └── src/
        ├── components/        # ResumeUpload, QuestionCard, StreamingFeedback
        ├── services/api.js    # Plain fetch calls + SSE stream reader
        └── App.jsx            # Main dashboard layout
```

---

## 3. How the Key Features Work (Easy to Explain)

### A. Resume Keyword Extraction (5 lines in Service)
- **Concept**: User uploads a PDF resume. Apache PDFBox extracts the raw text.
- **Code Logic**:
  ```java
  PDDocument doc = Loader.loadPDF(file.getBytes());
  String text = new PDFTextStripper().getText(doc);
  doc.close();
  // Pass extracted text into the AI system prompt
  ```

### B. Real-Time AI Feedback Streaming (SSE)
- **Concept**: User submits their interview answer. Spring Boot streams the AI response token-by-token using `SseEmitter` over HTTP.
- **Backend (`InterviewController.java`)**:
  ```java
  @GetMapping(value = "/stream-feedback", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamFeedback(@RequestParam String answer) {
      SseEmitter emitter = new SseEmitter(60000L);
      aiService.streamResponse(answer, emitter);
      return emitter;
  }
  ```
- **Frontend (`App.jsx`)**:
  Standard browser `EventSource` listening to `/api/v1/interviews/stream-feedback`.

### C. Standard CRUD & Progress
- Spring Data JPA manages users, courses, and interview history.
- Passwords hashed with standard BCrypt.

---

## 4. Required REST Endpoints
1. `POST /api/v1/auth/login` - Authenticate user, return JWT or user session.
2. `POST /api/v1/resume/upload` - Upload resume (multipart), return extracted profile summary.
3. `GET  /api/v1/courses` - Fetch interview prep stages (Self-Intro, Technical, HR).
4. `GET  /api/v1/interviews/stream` - SSE stream delivering real-time answer critique.
5. `GET  /api/v1/progress` - Fetch user level, XP, and streak.

---

## 5. Interview Cheat Sheet: How to Explain This Project
1. **Architecture**: *"I decoupled the monolithic Java desktop app into a standard 3-tier Spring Boot REST API paired with a modern React frontend."*
2. **AI Feature**: *"Instead of blocking HTTP calls where the user waits 10 seconds for feedback, I implemented Server-Sent Events (SSE) to stream tokens character-by-character as the model generates them."*
3. **Resume Personalization**: *"I used Apache PDFBox to parse candidate resumes in-memory and inject their skills directly into the LLM system prompt for tailored interview questions."*
4. **Security & Clean Code**: *"All secrets are injected via environment variables. The persistence layer uses Spring Data JPA with clear entity-to-DTO mapping."*

# TASK: Implement Token Streaming (SSE) and Resume RAG in 100% Java (Spring Boot + JavaFX)

## 1. Context & Goal
The project is decoupled into `interviewai-backend` (Spring Boot 3) and `interviewai-frontend` (JavaFX). 
Add two advanced engineering features while keeping the stack 100% Java (Java 17):
1. **Real-Time Token Streaming (Server-Sent Events / SSE)**: Stream AI interview critique character-by-character into the JavaFX UI.
2. **Resume RAG & Personalization**: Upload candidate CVs (PDF) via JavaFX, extract skills via Apache PDFBox in the backend, and dynamically generate personalized interview questions.

---

## 2. Feature 1: Token Streaming via SSE

### A. Backend (`interviewai-backend`)
* **Endpoint**: `GET /api/v1/interviews/evaluate/stream?questionId={id}&userAnswer={answer}`
* **Content-Type**: `text/event-stream`
* **Controller Implementation**:
  ```java
  @GetMapping(value = "/evaluate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvaluation(@RequestParam Long questionId, @RequestParam String userAnswer) {
      SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout
      aiService.streamEvaluation(questionId, userAnswer, emitter);
      return emitter;
  }
  ```
* **Service Layer**:
  - Connect to OpenRouter/Gemini streaming endpoint (`"stream": true`).
  - Read input stream line-by-line and push chunks to client via `emitter.send(chunk)`.
  - Handle `emitter.complete()` on finish and `emitter.completeWithError()` on failure.

### B. Frontend (`interviewai-frontend`)
* **Service Gateway**:
  - Use Java 17 `HttpClient` sending an asynchronous request (`sendAsync`).
  - Read incoming lines as an `InputStream`.
* **Thread Safety (Oracle Interview Focus)**:
  - Process chunks on a background worker thread.
  - Append tokens to the JavaFX `TextArea` / `TextFlow` using `Platform.runLater()` to keep the UI thread non-blocking and responsive.

---

## 3. Feature 2: Resume Ingestion & RAG Question Personalization

### A. Backend (`interviewai-backend`)
* **Endpoint**: `POST /api/v1/resumes/upload` (consumes `multipart/form-data`)
* **Processing**:
  - Parse uploaded PDF in-memory using Apache PDFBox:
    ```java
    try (PDDocument document = Loader.loadPDF(file.getBytes())) {
        String fullText = new PDFTextStripper().getText(document);
        return resumeService.extractSkillsAndGeneratePlan(fullText);
    }
    ```
  - Extract key technologies, seniority level, and candidate experience.
  - Store extracted context in user session / database.
* **Context Injection**:
  - Inject extracted skills directly into subsequent mock interview prompt templates so questions target the candidate's exact CV profile.

### B. Frontend (`interviewai-frontend`)
* Add a "Upload CV" button opening a standard JavaFX `FileChooser` (filtered to `.pdf`).
* Submit the file via `HttpClient` as a `multipart/form-data` POST request.
* Display detected candidate skills and unlocked personalized interview tracks.

---

## 4. Coding & Architecture Standards
1. **Explainability**: Keep code simple, concise, and clean. No unnecessary abstractions.
2. **Thread Safety**: Never block the JavaFX Application Thread with network I/O.
3. **Error Handling**: Gracefully handle SSE disconnects and PDF parsing exceptions with clear error messages.
4. **Security**: Validate that uploaded files are valid PDFs and do not exceed 5 MB.

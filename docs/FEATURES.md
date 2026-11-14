# Streaming critique and resume personalization

Both features run on Java 17. Configure the backend's `AI_API_KEY`, `AI_MODEL`, and optionally `AI_API_URL` as described in the root README. The URL must support OpenAI-compatible chat completions and SSE deltas (such as Google Gemini). Provider credentials stay on the backend.

## Desktop flow

1. In onboarding, select **Upload CV** and choose a PDF. Reading, uploading and profiling run asynchronously. Navigation is disabled while the upload is pending.
2. Review the detected skills, seniority, experience and suggested personalized interview tracks. The backend saves the profile and extracted evidence for the signed-in user.
3. Start practicing. Both technical and behavioral course generation retrieve this context to tailor questions. With no uploaded CV, ordinary preference-based generation still works.
4. Submit a lesson or review answer. Critique appears progressively in a read-only text area; the final score and feedback appear once the server validates and saves the evaluation. **Cancel evaluation**, leaving the view, or signing out cancels the client stream. Network reads run on background workers and UI updates use `Platform.runLater`.

## SSE contract

Authenticated `GET /api/v1/interviews/evaluate/stream?questionId={id}&userAnswer={answer}` returns `text/event-stream`. URL-encode the answer (maximum 12,000 characters). Ownership and input validation happen before opening the stream. Regular validation/authentication failures use the JSON error envelope.

```text
event:token
data:{"text":"You identified the key concept. "}

event:result
data:{"feedbackId":1,"score":85,"strengths":"Clear reasoning","improvements":"Add an example","explanation":"You identified the key concept.","correctAnswer":"...","status":"COMPLETED","xpAwarded":10}

```

`token` carries incremental critique text, not raw provider JSON. `result` carries the same evaluation object as the existing POST endpoint. The server parses one streaming provider response to obtain both critique and grade. Multiple-choice questions use deterministic grading and emit their explanation plus result without a provider call. Comment heartbeats arrive every 10 seconds; the stream has a 60-second deadline.

An interrupted or invalid provider response emits `event:error` with `{"message":"..."}` when the connection is still writable, then terminates. EOF without a result is an incomplete evaluation. Only a complete, validated grade reaches transactional persistence; existing once-per-question XP protection remains active. A disconnect after the database commit can lose the final event even though the evaluation was saved, so the client does not automatically retry.

Configure any reverse proxy to disable response buffering for this route and allow at least 60 seconds. The backend sends `Cache-Control: no-store` and `X-Accel-Buffering: no`. Its request-header limit accommodates URL-encoded answers. Because the required endpoint carries answers in its query string, exclude query strings from access logs.

## Resume API and persistence

`POST /api/v1/resumes/upload` consumes multipart field `file` and returns the normal JSON envelope containing:

```json
{"skills":["Java","Spring Boot"],"seniority":"Senior","experience":"5 years building Java APIs","tracks":["Java backend","API design"]}
```

`GET /api/v1/resumes/me` returns the signed-in user's profile, or `data: null` when none exists. No client-supplied user ID is accepted. A replacement is saved only after successful PDF extraction and profile validation; a failed upload preserves the previous context.

PDFBox runs only in the backend and parses the PDF in memory. The server checks size (5 MiB maximum), signature and actual PDF structure. Empty, encrypted, unreadable and textless/scanned PDFs receive clear errors. CVs are limited to 50 pages and 40,000 extracted characters. Oversize uploads return 413; invalid PDFs return 400. Scanned PDFs require OCR before upload.

Flyway V3 adds `resume_context`, keyed by user ID with cascade deletion. It stores extracted text and validated profile JSON; raw PDF files and desktop file paths are not stored. Generation retrieves the complete bounded resume and profile for that user and places them in the model context as untrusted evidence. This is per-user retrieval and prompt augmentation; there is no embedding service or vector database. The existing two-course structure is retained, with suggested tracks guiding personalization.

## Verification

Run `mvn verify` on Java 17. Tests exercise real PDF creation/extraction, invalid and oversized uploads, failed replacement, user isolation, context injection, SSE authorization and persistence, partial/escaped provider tokens, missing completion markers, provider errors, and HTTP multipart/SSE transport. A gated provider fixture verifies tokens arrive before generation completes; an embedded-server HTTP test checks that the actual SSE endpoint flushes before scoring completes. JavaFX tests load onboarding and check controller/FXML bindings.

AI responses are mocked or served by local HTTP fixtures. Live provider quality and latency require a configured provider key.

# InterviewAI

Interview preparation with an independent Spring Boot 3 REST backend and JavaFX desktop client.

```text
pom.xml                         Maven parent / Java 17
interviewai-backend/             REST API, JWT security, JPA, Flyway, AI services
interviewai-frontend/            JavaFX UI and Java HttpClient gateways
docs/MIGRATION_STATUS.md         Migration decisions and existing-database rollout
docs/legacy/                    Historical documentation and SQL dumps (not executed)
```

## Build and test

Requires Java 17+, Maven 3.9+, and MySQL 8 for running the backend. Tests use an isolated H2 database by default and do not require MySQL or an AI key.

```powershell
mvn verify
```

The backend is packaged as `interviewai-backend/target/interviewai-backend-0.2.0-SNAPSHOT.jar`. Both modules compile to Java 17 bytecode. The frontend has no JDBC driver, database credentials, or provider credentials.

## Start the backend

Create an empty MySQL database named `interviewai` and a database user allowed to run the Flyway migrations. Set these variables in the backend's shell or IDE:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/interviewai?connectionTimeZone=UTC'
$env:DB_USERNAME = 'interviewai' 
$env:DB_PASSWORD = '<your database password>'
# Generate a random signing secret; keep it stable across restarts.
$bytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:AI_API_KEY = '<your OpenRouter key>'
$env:AI_MODEL = 'openai/gpt-4o-mini'
mvn -pl interviewai-backend spring-boot:run
```

Alternatively, after building: `java -jar interviewai-backend/target/interviewai-backend-0.2.0-SNAPSHOT.jar`.

Flyway creates the schema on an empty database; Hibernate validates it. For an existing database, follow [the migration rollout](docs/MIGRATION_STATUS.md#database-rollout) before starting the backend. Automatic baselining is disabled. Historical database dumps are not startup scripts.

The key is optional for startup. Resume profiling, short-answer evaluation and course generation require `AI_API_KEY`; multiple-choice evaluation and other endpoints work without AI. Other settings appear in [.env.example](.env.example). Export them explicitly: Spring Boot does not load that example file automatically.

API documentation: [Swagger UI](http://localhost:8080/swagger-ui.html) and [OpenAPI JSON](http://localhost:8080/v3/api-docs). AI calls use Spring's [RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html); JWT verification uses [Spring Security's resource-server support](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## Start the desktop client

In a second shell:

```powershell
$env:INTERVIEWAI_API_URL = 'http://localhost:8080/api/v1'
mvn -pl interviewai-frontend javafx:run
```

Create an account through the registration screen. There is no seeded demo administrator. The client keeps the JWT in memory and clears it when signing out. Server operations use the authenticated user, not client-provided user IDs.

To bootstrap the first administrator, register a normal account, then run this statement as a trusted database administrator, replacing the username with that account's name:

```sql
UPDATE users SET role = 'ROLE_ADMIN', token_version = token_version + 1
WHERE username = '<registered administrator username>';
```

Sign in again afterward. Further account management uses the authenticated admin API/UI.

## REST API

JSON responses use `{success, message, data, timestamp}`. The streaming endpoint uses SSE events instead. Only registration, login and OpenAPI documentation are public. Other endpoints require `Authorization: Bearer <accessToken>`; `/admin/**` additionally requires `ROLE_ADMIN`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Register with username, email and password |
| POST | `/api/v1/auth/login` | Sign in with username/email and password |
| GET | `/api/v1/courses` | Current user's courses |
| GET | `/api/v1/courses/{id}/lessons` | Lessons in an owned course |
| POST | `/api/v1/interviews/evaluate` | Evaluate `{questionId, userAnswer}` |
| GET | `/api/v1/interviews/evaluate/stream?questionId={id}&userAnswer={answer}` | Live critique followed by a saved evaluation |
| POST | `/api/v1/resumes/upload` | Upload a PDF using multipart field `file` |
| GET | `/api/v1/resumes/me` | Current user's extracted resume profile |
| GET | `/api/v1/users/me` | Current user's profile |
| GET | `/api/v1/quests` | Quest progress and claim status |
| GET | `/api/v1/admin/users` | User administration |
| GET | `/api/v1/admin/analytics` | Server-calculated analytics |

Additional routes support onboarding and generation, lesson questions, preparation pairs, profile/password changes, goals, achievements, leaderboards, quest claims, and administration. See OpenAPI for request schemas.

Passwords use BCrypt. Legacy SHA-256 hashes upgrade upon successful login. Access tokens expire after one hour by default; account suspension, password changes and role changes invalidate existing tokens. Rewards and completion are calculated by the backend, including duplicate/concurrent reward protection. Dates and quest periods use UTC.

## Features and architecture

- **Resume Ingestion & Personalization**: The onboarding screen accepts candidate CVs (PDF up to 5 MB), extracting skills, experience, and seniority in-memory via Apache PDFBox to tailor course questions.
- **Real-Time Token Streaming**: Lesson and review screens stream AI critique character-by-character over HTTP Server-Sent Events (SSE) with interactive cancellation. See [feature behavior and SSE protocol](docs/FEATURES.md).
- **Migration & Architecture**: See [architecture status and rollout](docs/MIGRATION_STATUS.md) for data mappings, compatibility details, and test instructions. Older documentation and legacy SQL snapshots are retained under `docs/legacy` for reference.

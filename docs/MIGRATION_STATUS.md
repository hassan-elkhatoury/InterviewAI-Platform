# Architecture Migration Implementation

The architectural decoupling was implemented in five structured steps:

1. **Parent module:** root Maven reactor, Java 17 release target, Spring Boot 3.5 dependency management, separate backend/frontend modules.
2. **Backend scaffold:** standalone executable Spring Boot API with Web, JPA, Security, Validation, MySQL, Flyway and OpenAPI.
3. **Persistence:** JPA entities/repositories map the existing tables. `Course` maps to `generated_courses`; `Lesson` maps to `chapters`. Additional entities cover questions, choices, progress, onboarding, goals, preparations, quests, claims and achievements. Historical SQL is retained in `docs/legacy/db`, outside the application classpath.
4. **Services and controllers:** required v1 endpoints, validated request records, response envelopes, BCrypt/JWT authentication, current database role checks, ownership checks, global error handling, RestClient AI integration, transactional evaluation/rewards, achievements and server-side quest eligibility.
5. **Frontend isolation:** existing FXML/CSS/images/controllers relocated under `interviewai-frontend`; packages use `com.interviewai.client`. Java `HttpClient` gateways replace JDBC. Answer evaluation and course generation use the backend. The desktop stores its JWT only in memory and clears it on logout/401.

## Behavior and compatibility

- Public registration always assigns `ROLE_USER`. Admin-only operations require `ROLE_ADMIN` and never trust a client-supplied identity.
- Legacy `ADMIN` / `SUPER_ADMIN` users and members of the `SUPER_ADMIN` role become `ROLE_ADMIN`. Other legacy roles become `ROLE_USER`; review their access before switching users over. The old permissions tables remain intact as historical data.
- Existing BCrypt hashes remain valid. SHA-256 legacy passwords upgrade to BCrypt after successful authentication. Password and access changes invalidate existing tokens.
- Correct answers earn the existing lesson reward of 10 XP once per question. The server records feedback, progress, lesson/course completion and achievements transactionally. AI outages return errors, with no fabricated scores or XP.
- Daily quests require 20 distinct answered questions and award 900 XP once per UTC day. Monthly quests require 12 completed lessons and award 30,000 XP once per UTC month. Legacy claim dates are migrated. Historical XP is retained; AI-score analytics count evaluations recorded by the new API.
- Course generation creates technical and behavioral courses, each requested with 12 chapters and 3 questions per chapter, and links them atomically. Provider output is validated before persistence. PDF uploads are extracted in memory by the backend and stored per user; subsequent generation retrieves this context. The server never opens a client-supplied filesystem path.
- AI configuration belongs only to the backend. A blank key permits non-AI endpoints to run; AI requests return 503 until configured.
- The backend uses Spring Security's Bearer token filter and Nimbus JWT encoder/decoder rather than a handwritten token parser.

## Database rollout

No existing database is automatically baselined. A fresh, empty database receives V1, V2 and V3 on startup. V3 adds per-user resume context; databases already migrated through V2 receive V3 automatically.

For an existing installation:

1. Stop writes from the old desktop application and take a tested backup.
2. Compare its schema with `V1__legacy_schema.sql`. Older variants such as `schema2.sql` are not interchangeable. Reconcile missing legacy columns/tables before baselining; do not run old dump `DROP TABLE` statements against live data.
3. Restore the backup into a staging database and point `DB_URL` at that copy.
4. Start the backend once with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=1`. This explicitly records the existing schema as V1 and applies subsequent migrations. Remove the baseline overrides afterward.
5. Verify users, roles, courses, lessons, XP and existing quest claims, then repeat the reviewed procedure for the intended database. MySQL DDL is not fully transactional; restore the backup if a migration fails partway through.

The legacy local `src/main/resources/config/config.properties`, if present, remains ignored and is not packaged or read. Set environment variables as described in the root README.

## Validation

`mvn verify` runs the backend integration suite against an isolated in-memory database using the real Flyway scripts and schema validation. Tests cover credentials, validation, JWT/role/ownership checks, evaluation and AI failures, duplicate/concurrent rewards, quest eligibility, profile/goals/onboarding, generated courses, and upgrading a legacy schema. The frontend suite exercises real HTTP transport, token handling and JSON compatibility.

To run the same integration tests against a **disposable, empty MySQL database**, set `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` before `mvn verify`. Tests create records and run schema migrations, so these variables must never point to production.

Live AI-provider calls require your own `AI_API_KEY`; tests use local HTTP fixtures and mocked generation/evaluation responses.

The original migration was verified locally on Java 17.0.21 with 26 passing tests (20 backend, 6 frontend), including actual JavaFX loading of the login and registration screens. The 13 API integration tests also passed against an isolated MySQL 8.0.45 database with V1/V2 Flyway migrations and Hibernate schema validation. The existing application database was not modified.

Following the feature expansion, the Java 17 Maven reactor passed 42 tests (33 backend, 9 frontend). This includes V3 migration/schema validation on H2, PDF ingestion and personalization, live SSE delivery over an embedded HTTP server, provider streaming fixtures, client cancellation, and loading the updated onboarding screen. See [feature details](FEATURES.md).

The packaged executable backend JAR also passed an HTTP smoke test against that MySQL instance: OpenAPI listed all required endpoints; registration, profile, courses, quests and progress succeeded; unauthenticated and non-admin requests returned 401 and 403 respectively.

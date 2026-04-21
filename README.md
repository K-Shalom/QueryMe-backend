# QueryMe Backend

Spring Boot backend for QueryMe, a web-based SQL exam platform for database courses.

The backend is organized as a modular monolith around the intended project flow:

- teachers create exams and questions
- each student attempt gets a private PostgreSQL schema
- student SQL scripts are graded by final result-set comparison, not query-string matching
- results are exposed according to the exam visibility mode

This README is backend-focused and grouped by the team/module ownership described in the project brief.

## Architecture Summary

- Backend: Spring Boot modular monolith
- Auth: Spring Security + JWT
- App data source: `spring.datasource`
- Sandbox data source: `queryme.sandbox-datasource.*`
- Roles in code: `TEACHER`, `STUDENT`, `ADMIN`, `GUEST`
- Server port: `8085` (default, configurable with `APP_PORT`)

The sandbox connection now supports a separate PostgreSQL database, which matches the intended `queryme_app` / `queryme_sandbox` split. For local development, the sandbox data source falls back to the main data source if sandbox-specific environment variables are not set.

## Run Locally

Start the app with:

```bash
./mvnw spring-boot:run
```

Current config comes from `src/main/resources/application.yml` plus the optional `.env` file.

Required environment variables:

- `DB_HOST`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Optional auth environment variables:

- `JWT_SECRET`
- `JWT_EXPIRATION`

Optional sandbox database environment variables:

- `SANDBOX_DB_URL`
- `SANDBOX_DB_USER`
- `SANDBOX_DB_PASSWORD`
- `SANDBOX_DB_DRIVER`

Optional security environment variables:

- `CORS_ALLOWED_ORIGINS` comma-separated allowed origins for API CORS

Optional advanced sandbox isolation variables:

- `SANDBOX_DB_USER_ISOLATION_ENABLED` enable per-sandbox DB-role provisioning (PostgreSQL only)
- `SANDBOX_DB_USER_PREFIX` username prefix for generated sandbox DB users
- `SANDBOX_DB_USER_PASSWORD_LENGTH` generated sandbox DB user password length

## Run with Docker Compose

To build and run the application using Docker and Docker Compose, follow these steps:

1. **Build the application JAR:**
   Since the `Dockerfile` copies the packaged JAR file, you must first build the Spring Boot application using Maven:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Start the containers:**
   Use Docker Compose to build the Docker image and start the application in the background:
   ```bash
   docker compose up -d --build
   ```

3. **Stop the containers:**
   To stop and remove the running containers:
   ```bash
   docker compose down
   ```

The application will be accessible at `http://localhost:<APP_PORT>` (where `APP_PORT` is defined in your `.env` file).

## Alignment Highlights

These backend changes now match the intended QueryMe flow more closely:

- students only see published exams assigned to them by course or enrollment
- student-facing exam responses no longer expose `seedSql`
- student-facing question responses no longer expose `referenceQuery`
- the first super admin can be bootstrapped once through a public auth endpoint
- question create and update both regenerate the answer key immediately
- sessions now enforce `maxAttempts`
- starting a session provisions the sandbox automatically
- session expiry is auto-submitted on a scheduler and tears the sandbox down
- query submissions are tied to a session, not just an exam/student pair
- query submissions can now include sandbox-scoped write and DDL statements without affecting other sandboxes or system schemas
- query submit responses only include marks/result sets when the exam visibility mode allows it
- query validation and execution failures now return through `SubmissionResponse.executionError` instead of surfacing as rollback-only transaction errors
- student result views are built from the latest submission per question for that session
- manual sandbox provisioning now returns the real configured connection info instead of a hard-coded DB user
- query execution now adapts common MySQL syntax (`\`` identifiers, `IFNULL`, `LIMIT offset,count`, and simple MySQL DDL table options) to PostgreSQL-compatible SQL before validation/execution
- final `INSERT`/`UPDATE`/`DELETE` statements now auto-append `RETURNING *` when omitted, so DML questions can still produce a comparable result set

## Recent Performance Changes (April 2026)

This backend received a performance-focused update. No brand-new API paths were introduced, but several existing endpoints and internals were improved for lower latency and better scaling.

### What Changed Internally

- reduced repeated authenticated-user lookups by reusing JWT principal details in `CurrentUserService`
- optimized exam listing to avoid per-exam question-count queries by batching question counts
- optimized teacher dashboard assembly by using projection queries (lightweight row views) instead of full entity loading
- optimized submission processing to reuse the active session sandbox schema when available
- reduced object allocations in partial-mark scoring logic
- disabled verbose SQL logging in runtime config to reduce request-path I/O noise

### Database Indexes Added


Indexes were added via JPA entity metadata and are applied by Hibernate with the current `ddl-auto: update` strategy.

- `submissions`
  - `(exam_id, submitted_at)`
  - `(session_id, submitted_at)`
  - `(student_id, exam_id)`
  - `(question_id)`
- `exam_sessions`
  - `(exam_id)`
  - `(student_id)`
  - `(exam_id, student_id, started_at)`
  - `(submitted_at, expires_at)`
- `questions`
  - `(exam_id, order_index)`
  - `(exam_id)`
- `exams`
  - `(course_id)`
  - `(status)`
  - `(course_id, status)`
- `results`
  - `(submission_id)`
  - `(session_id)`
  - `(exam_id, question_id)`
- `sandbox_registry`
  - `(exam_id, student_id)`
  - `(status, expires_at)`

### API Behavior Updates

- list-style endpoints now support Spring pageable query params: `page`, `size`, `sort`
- these endpoints now return paged JSON objects (`Page<T>`) instead of raw arrays
- endpoint paths are unchanged (no new route paths were added for pagination)

Paged endpoints:

- `GET /students`
- `GET /teachers`
- `GET /admins`
- `GET /guests`
- `GET /courses`
- `GET /class-groups`
- `GET /class-groups/course/{courseId}`
- `GET /course-enrollments`
- `GET /course-enrollments/course/{courseId}`
- `GET /course-enrollments/student/{studentId}`
- `GET /exams/{examId}/questions`
- `GET /sessions/exam/{examId}`
- `GET /sessions/student/{studentId}`

Paged response shape follows Spring Data defaults, including fields such as:

- `content`
- `number`
- `size`
- `totalElements`
- `totalPages`
- `first`
- `last`

Example:

```http
GET /students?page=0&size=20&sort=id,desc
```

## Group Ownership Map

| Group | Module | Responsibility |
|---|---|---|
| Group J | Auth | Authentication, JWT generation/validation, and route security |
| Group F | User and student management | Student/teacher identity, courses, class groups, and enrollment |
| Group A | Exam module | Exam lifecycle, settings, publishing, attempt limits, and visibility |
| Group I | Question module | Question CRUD and answer-key generation from the teacher reference query |
| Group D | Sandbox module | Session lifecycle, sandbox schema creation, seeding, teardown, and cleanup |
| Group G | Query engine | SQL validation, execution, result comparison, and submission scoring |
| Group C | Results module | Student-visible results and teacher dashboard aggregation |

## Security Snapshot

- JWT auth is enforced by `JwtAuthFilter`.
- Method security is enabled through `@EnableMethodSecurity`.
- Scheduling is enabled through `@EnableScheduling`.
- API request validation and centralized JSON error handling are enabled.
- CORS allowlist is controlled by `queryme.security.cors.allowed-origins`.
- Public endpoints in the current config are:
  - `POST /auth/signin`
  - `POST /auth/signup`
  - `POST /auth/bootstrap/super-admin`
  - `GET /courses`
  - `GET /class-groups/**`
- Public signup only supports student registration; elevated roles are not public-signup capable.
- Super admin bootstrap is only allowed while no `Admin` record is flagged as `superAdmin`.
- Teacher/admin writes are enforced for exam mutation and course enrollment endpoints.
- Students can start and submit their own sessions, but exam-wide session listings are teacher/admin only.
- Manual sandbox endpoints are teacher/admin only.

Authorization header format:

```text
Authorization: Bearer <jwt>
```

## Group J - Auth

Base path: `/auth`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/auth/signup` | Registers a student user with shared auth credentials. Accepts student fields; non-student roles are rejected. | Public |
| `POST` | `/auth/signin` | Authenticates a user and returns the JWT payload. | Public |
| `POST` | `/auth/bootstrap/super-admin` | Creates the first super admin account if none exists yet. Returns an error once a super admin has already been initialized. | Public |

Notes:

- Signup creates credentials in the shared `users` auth table.
- Public signup creates `STUDENT` accounts only.
- Super admin bootstrap creates an `ADMIN` user plus an `Admin` profile with `superAdmin = true`.
- Teacher/admin/guest accounts are restricted to admin-managed registration endpoints.

## Group F - User and Student Management

These endpoints own profile records, course structure, and teacher-driven student assignment.

### Student and Teacher Profiles

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/teachers/register` | Creates a teacher profile. | `ADMIN` |
| `PUT` | `/teachers/{id}` | Updates a teacher profile. | `TEACHER` or `ADMIN` |
| `GET` | `/teachers` | Lists teachers. | `TEACHER` or `ADMIN` |
| `POST` | `/students/register` | Creates a student profile. | `TEACHER` or `ADMIN` |
| `POST` | `/students/register/bulk` | Creates multiple student profiles atomically from an array of student registration payloads. | `TEACHER` or `ADMIN` |
| `PUT` | `/students/{id}` | Updates student profile fields such as name, password, course, and class group. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/students` | Lists students. | `TEACHER` or `ADMIN` |
| `GET` | `/students/{id}` | Fetches one student profile by numeric student-table id. | `STUDENT`, `TEACHER`, or `ADMIN` |

### Courses, Class Groups, and Enrollment

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/courses` | Creates a course linked to the logged-in teacher. | `TEACHER` |
| `GET` | `/courses` | Lists all courses. | Public |
| `POST` | `/class-groups` | Creates a class group. | `TEACHER` |
| `GET` | `/class-groups` | Lists all class groups. | Public |
| `GET` | `/class-groups/course/{courseId}` | Lists class groups for one course. | Public |
| `POST` | `/course-enrollments` | Enrolls a student into a course. Accepts `courseId` and `studentId` as query params or JSON body fields; legacy `course_id` and `student_id` body fields still work. | `TEACHER` or `ADMIN` |
| `GET` | `/course-enrollments` | Lists all enrollments. | `TEACHER` or `ADMIN` |
| `GET` | `/course-enrollments/course/{courseId}` | Lists enrollments for one course. | `TEACHER` or `ADMIN` |
| `GET` | `/course-enrollments/student/{studentId}` | Lists enrollments for one student. | `TEACHER` or `ADMIN` |
| `DELETE` | `/course-enrollments` | Removes an enrollment. Accepts the same identifier formats as the create endpoint. | `TEACHER` or `ADMIN` |

Student registration payloads support:

- `email`
- `password`
- `fullName`
- optional `courseId`
- optional `classGroupId`
- optional `student_number` or `studentNumber`

`POST /students/register/bulk` accepts a JSON array of those payloads and rolls the whole request back if any student in the batch fails validation or hits a duplicate email/student number.

### Additional Account Endpoints Present in the Codebase

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/admins/register` | Creates a regular admin profile. | `ADMIN` |
| `PUT` | `/admins/{id}` | Updates an admin profile. | `ADMIN` |
| `GET` | `/admins` | Lists admins. | `ADMIN` |
| `POST` | `/guests/register` | Creates a guest profile. | `ADMIN` |
| `PUT` | `/guests/{id}` | Updates a guest profile. | `GUEST` |
| `GET` | `/guests` | Lists guests. | `GUEST` |

Notes:

- Course assignment now matters to the exam flow: student exam visibility is filtered by direct course linkage and course enrollments.
- Credentials live in the shared auth model; the profile modules own the rest of the identity data.
- The first super admin is created through `/auth/bootstrap/super-admin`; later `/admins/register` calls create non-super-admin accounts.

## Group A - Exam Module

Base path: `/exams`

Exam lifecycle in the current service:

```text
DRAFT -> PUBLISHED -> CLOSED
  ^        |
  |        v
  +---- UNPUBLISH
```

`CreateExamRequest` supports:

- `courseId`
- `title`
- `description`
- `visibilityMode` as `IMMEDIATE`, `END_OF_EXAM`, or `NEVER`
- `timeLimitMins`
- `maxAttempts`
- `seedSql`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/exams` | Creates a draft exam. | `TEACHER` or `ADMIN` |
| `GET` | `/exams/{examId}` | Fetches one exam. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/exams/course/{courseId}` | Lists exams for one course. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/exams/published` | Lists published exams visible to the current user. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `PUT` | `/exams/{examId}` | Updates a draft exam. | `TEACHER` or `ADMIN` |
| `PATCH` | `/exams/{examId}/publish` | Publishes a draft exam. | `TEACHER` or `ADMIN` |
| `PATCH` | `/exams/{examId}/unpublish` | Moves a published exam back to draft. | `TEACHER` or `ADMIN` |
| `PATCH` | `/exams/{examId}/close` | Closes a published exam. | `TEACHER` or `ADMIN` |
| `DELETE` | `/exams/{examId}` | Deletes a draft exam. | `TEACHER` or `ADMIN` |

Notes:

- Students only receive exams that are both `PUBLISHED` and assigned to them.
- Student-facing `ExamResponse` values hide `seedSql`.
- `maxAttempts` now defaults to `1` if omitted.
- Only `DRAFT` exams can be edited or deleted.
- Publishing requires both `seedSql` and `visibilityMode`.

## Group I - Question Module

Base path: `/exams/{examId}/questions`

When a teacher creates or updates a question, the service:

1. saves the question
2. provisions a temporary sandbox seeded from the exam dataset
3. runs the teacher's `referenceQuery` script inside that sandbox
4. stores the normalized answer key JSON from the final returned result set
5. rolls back reference-script mutations before keeping or cleaning up the preview sandbox

`QuestionRequest` supports:

- `prompt`
- `referenceQuery`
- `marks`
- `orderIndex`
- `orderSensitive`
- `partialMarks`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/exams/{examId}/questions` | Creates a question and generates the answer key immediately. | `TEACHER` or `ADMIN` |
| `PUT` | `/exams/{examId}/questions/{questionId}` | Updates a question and regenerates the answer key immediately. | `TEACHER` or `ADMIN` |
| `GET` | `/exams/{examId}/questions` | Lists questions ordered by `orderIndex`. | `STUDENT`, `TEACHER`, or `ADMIN` |

Notes:

- `referenceQuery` can be a sandbox-scoped multi-statement script.
- for DML reference queries (`INSERT`/`UPDATE`/`DELETE`), the backend automatically appends `RETURNING *` to the final statement when omitted.
- Students can only fetch questions for exams that are published and assigned to them.
- Student-facing question payloads hide `referenceQuery`.
- If answer-key generation fails, question creation or update fails as well.

## Group D - Sandbox Module

The runtime exam attempt flow is split into session endpoints and direct sandbox support endpoints.

### Session Lifecycle

Base path: `/sessions`

`StartSessionRequest` supports:

- `examId`
- `studentId`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/sessions/start` | Starts a session, validates assignment, provisions the sandbox, and stores the real schema name. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `PATCH` | `/sessions/{sessionId}/submit` | Submits the session and tears the sandbox down. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/sessions/{sessionId}` | Returns one session. Students are limited to their own sessions. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/sessions/student/{studentId}` | Lists sessions for one student. Students are limited to their own sessions. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/sessions/exam/{examId}` | Lists sessions for one exam. | `TEACHER` or `ADMIN` |

Session response fields include:

- `id`
- `examId`
- `studentId`
- `startedAt`
- `submittedAt`
- `expiresAt`
- `sandboxSchema`
- `isSubmitted`
- `isExpired`

Current behavior:

- a student can only have one active session per exam at a time
- `maxAttempts` is enforced from the exam definition
- session start requires the exam to be `PUBLISHED`
- session expiry is auto-submitted by a scheduler running every 60 seconds
- expired sessions encountered during reads are also auto-submitted
- session submit and auto-submit both tear the sandbox down

### Direct Sandbox Endpoints

Base path: `/sandboxes`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/sandboxes/provision` | Provisions or reuses a sandbox schema and seeds it with the supplied SQL. | `TEACHER` or `ADMIN` |
| `GET` | `/sandboxes/{examId}/students/{studentId}` | Returns sandbox connection details for an active schema. | `TEACHER` or `ADMIN` |
| `DELETE` | `/sandboxes/{examId}/students/{studentId}` | Drops the sandbox schema and marks it dropped in the registry. | `TEACHER` or `ADMIN` |

Notes:

- The normal student flow should use `/sessions/start`, not manual sandbox provisioning.
- Sandbox endpoints take the numeric student profile id and resolve it to the auth user UUID internally.
- Sandbox schema names now follow the exam/student identity pattern instead of relying on a local README-only sequence.
- Sandbox connection info comes from the configured sandbox data source.
- Optional hardening mode can provision a dedicated PostgreSQL role per sandbox (`SANDBOX_DB_USER_ISOLATION_ENABLED=true`) with schema-local DML/DDL permissions, restricted sandbox access, and automatic role cleanup on teardown.

## Group G - Query Engine

Base path: `/query`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/query/submit` | Grades one student SQL submission against the answer key. | `STUDENT`, `TEACHER`, or `ADMIN` |

`SubmissionRequest` supports:

- `sessionId` optional
- `examId`
- `questionId`
- `studentId`
- `query`

What happens during grading:

1. the service validates that the question belongs to the supplied exam
2. the active exam session is resolved and validated
3. the student's sandbox is resolved
4. the SQL is validated against sandbox-safety rules
5. the SQL script runs atomically inside the student's sandbox with a hard 10-second timeout
6. the final returned result set is compared to the stored answer key
7. a submission row is saved with score, correctness, and captured result-set JSON
8. the results module is notified so the `results` table stays synchronized

Submission SQL constraints in current implementation:

- allowed commands include `SELECT`, `WITH`, `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `CREATE TABLE/VIEW/INDEX`, `ALTER TABLE`, and `DROP TABLE/VIEW/INDEX`
- multi-statement submissions are allowed
- every referenced object must stay inside the current student's sandbox schema
- `public`, `information_schema`, `pg_*`, and other students' `exam_*` schemas are blocked
- SQL comments and dollar-quoted blocks are rejected
- system-level commands such as `GRANT`, `REVOKE`, `COPY`, `VACUUM`, `CALL`, and similar commands are blocked
- temporary and unlogged objects are blocked

MySQL compatibility in current implementation:

- backtick-quoted identifiers are converted to PostgreSQL double-quoted identifiers before validation
- `IFNULL(expr, fallback)` is converted to `COALESCE(expr, fallback)`
- MySQL `LIMIT offset, count` is converted to PostgreSQL `LIMIT count OFFSET offset`
- basic MySQL table options in `CREATE TABLE` (for example `ENGINE=...`) are stripped for PostgreSQL execution
- when the final statement is `INSERT`, `UPDATE`, or `DELETE` without `RETURNING`, the backend appends `RETURNING *` automatically

Teacher authoring checklist (MySQL-first cohorts):

- Prefer standard SQL where possible (`SELECT`, `JOIN`, `GROUP BY`, `ORDER BY`, `INSERT`, `UPDATE`, `DELETE`)
- MySQL-style backticks and `IFNULL` are supported, but avoid relying on MySQL-only procedural features
- For pagination examples, `LIMIT offset, count` is accepted and auto-translated
- For DML questions, you can omit `RETURNING`; the backend adds `RETURNING *` on the final DML statement
- Keep each question deterministic and independent; avoid side effects that change expected outputs for later questions
- Avoid unsupported patterns such as SQL comments, dollar-quoted blocks, and system-level statements (`GRANT`, `REVOKE`, `COPY`, `VACUUM`, `CALL`, and similar commands)
- This remains a PostgreSQL execution sandbox; compatibility is best-effort for common MySQL learning syntax, not full MySQL runtime behavior

Current scoring behavior:

- exact match of the final returned result set: full marks
- `partialMarks = true` and matching row count: half marks
- validation failure, timeout, or execution failure: returned as `executionError`
- if the final student statement is `INSERT`/`UPDATE`/`DELETE` without `RETURNING`, the backend adds `RETURNING *` automatically before execution and grading

`SubmissionResponse` includes:

- `submissionId`
- `sessionId`
- `isCorrect`
- `score`
- `executionError`
- `resultsVisible`
- `resultColumns`
- `resultRows`

Visibility behavior on submit:

- `IMMEDIATE`: returns score, correctness, and result-set data right away
- `END_OF_EXAM`: saves the submission, but withholds marks and result rows from the submit response
- `NEVER`: saves the submission, but withholds marks and result rows from the submit response

Additional submit behavior:

- sandbox validation and execution failures are reported in a normal `SubmissionResponse` instead of bubbling up as rollback-only transaction errors

## Group C - Results Module

Base path: `/results`

| Method | Path | What it does | Access |
|---|---|---|---|
| `GET` | `/results/session/{sessionId}` | Returns the student-facing result view for a single session. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/results/exam/{examId}/dashboard` | Returns the teacher dashboard rows for an exam. | `TEACHER` or `ADMIN` |

### Student Result View

`GET /results/session/{sessionId}` now returns `StudentExamResultDto`, not raw `Result` entities.

Top-level fields:

- `sessionId`
- `examId`
- `studentId`
- `visibilityMode`
- `visible`
- `totalScore`
- `totalMaxScore`
- `questions`

Each question row includes:

- `questionId`
- `prompt`
- `submittedQuery`
- `score`
- `maxScore`
- `isCorrect`
- `submittedAt`
- `resultColumns`
- `resultRows`

Current visibility behavior:

- `IMMEDIATE`: visible immediately after grading
- `END_OF_EXAM`: visible once the student's attempt has ended by submission or expiry, and also visible after the exam is closed
- `NEVER`: hidden from the student view

### Teacher Dashboard

`GET /results/exam/{examId}/dashboard` returns one row per latest student submission per question:

- `studentId`
- `studentName`
- `sessionId`
- `questionId`
- `questionPrompt`
- `score`
- `maxScore`
- `isCorrect`
- `submittedQuery`
- `submittedAt`

Notes:

- result rows are synchronized automatically when a new query submission is saved
- the dashboard aggregates from the latest submission per student/question pair

## Current Backend Notes

These are still worth knowing while working on the backend:

- `GET /students/{id}` enforces student self-access while teacher/admin can read any student
- partial marking is intentionally simple right now: half marks when row count matches and `partialMarks` is enabled
- there is still no dedicated backend feature for revealing teacher reference queries to students after an exam, which matches the project brief's nice-to-have scope rather than its must-have scope

## Frontend Migration Checklist

Use this when wiring the frontend to the current backend shape.

- update list views to read `content` from Spring `Page<T>` responses instead of treating responses as raw arrays
- read pagination metadata from `number`, `size`, `totalElements`, and `totalPages` when building list tables
- pass `page`, `size`, and `sort` query params to the paged endpoints when fetching large lists
- keep using the existing endpoint paths; only the response shape changed for list endpoints
- handle hidden student fields correctly:
  - `seedSql` is still hidden from student-facing exam responses
  - `referenceQuery` is still hidden from student-facing question responses
- render teacher dashboard rows from `GET /results/exam/{examId}/dashboard` as a paged or filtered table if the result set becomes large in the UI layer
- prefer human-readable course, exam, and student names in the portal UI instead of exposing internal numeric IDs directly

Useful examples:

```http
GET /students?page=0&size=20&sort=id,desc
GET /exams/published?page=0&size=10&sort=createdAt,desc
GET /sessions/exam/{examId}?page=0&size=25
```

## QueryMe Feedbacks - Group J (Auth) - Implementation Summary

This section documents the security and user management enhancements implemented in April 2026.

### 1. Auto-generated Temporary Passwords & Real Email Delivery
- **Feature**: Admins can now register users without defining a password.
- **Implementation**: 
    - `PasswordService` generates a secure 16-character temporary password.
    - `EmailService` sends this password to the user's real email address using **Spring Mail** (configured via `.env`).
- **How to test**:
    - **Endpoint**: `POST /students/register`
    - **Payload**: Provide `email` and `fullName` but leave out the `password` field.
    - **Result**: The user receives an email with their credentials, and a fallback copy is printed to the server logs.

### 2. Forced Password Reset (First Login)
- **Feature**: Users who receive a temporary password must change it before they can use the system.
- **Implementation**: 
    - Login via `POST /auth/signin` returns `"mustResetPassword": true`.
    - This flag is cleared automatically after the user completes a profile update with a new password.
- **How to test**:
    - Sign in with a temporary password from Step 1.
    - Observe the `mustResetPassword` flag in the response JSON.

### 3. Strict Admin-Only Registration
- **Feature**: Only the `ADMIN` role can create new user profiles. Teachers no longer have registration rights.
- **Implementation**: Security rules in `SecurityConfig` and `@PreAuthorize` annotations in controllers.
- **How to test**:
    - Try calling `POST /students/register` using a `TEACHER` token.
    - **Result**: `403 Forbidden`.

### 4. Password History & Reuse Prevention
- **Feature**: Users cannot reuse any of their previously used passwords.
- **Implementation**: `PasswordHistory` entity records all previous password hashes. Checks are enforced during every password update.
- **How to test**:
    - Call `PUT /students/{id}` with a new password (e.g., `NewPass123!`).
    - Attempt a second update with the same password (`NewPass123!`).
    - **Result**: `400 Bad Request` with message: `"Cannot reuse a previous password"`.

### 5. Restricted Public Signup
- **Feature**: The public `/auth/signup` endpoint is now strictly reserved for students.
- **How to test**:
    - Call `POST /auth/signup` with `"role": "TEACHER"`.
    - **Result**: Rejected with `"Error: Public signup only supports STUDENT accounts"`.

### 6. Admin Student Deletion
- **Feature**: Added a new administrative endpoint to remove students and their linked user accounts.
- **How to test**:
    - **Endpoint**: `DELETE /students/{id}`
    - **Access**: `ADMIN` only.
    - **Result**: Student profile and user credentials are removed via cascading deletion.

### 7. Super Admin Reset (Utility)
- **Feature**: Added a temporary utility to reset the super admin status if credentials are lost.
- **Endpoint**: `POST /auth/bootstrap/reset` (Public).

### API Endpoint Summary Table (New & Updated)

| Method | Endpoint | Implementation Detail | Access |
|---|---|---|---|
| `POST` | `/auth/signin` | Returns JWT and `mustResetPassword` status | Public |
| `POST` | `/auth/signup` | Self-registration, restricted to `STUDENT` | Public |
| `POST` | `/auth/bootstrap/reset` | Resets super admin status for re-bootstrapping | Public |
| `POST` | `/students/register` | Creates student + temp password + email | `ADMIN` only |
| `DELETE` | `/students/{id}` | Permanently removes student and user account | `ADMIN` only |
| `PUT` | `/students/{id}`| Updates profile and enforces password history | Owner/`ADMIN` |
| `POST` | `/teachers/register`| Creates teacher + temp password + email | `ADMIN` only |
| `POST` | `/guests/register`| Creates guest + temp password + email | `ADMIN` only |
| `POST` | `/courses` | Creates a new course and assigns a teacher | `ADMIN` only |
| `POST` | `/course-enrollments` | Enrolls a student into a course | `ADMIN` only |
| `GET` | `/courses` | Lists courses (filtered for teachers) | `TEACHER`/`ADMIN` |
| `GET` | `/students` | Lists students (filtered for teachers) | `TEACHER`/`ADMIN` |

## Security & Data Isolation Model

The system implements a strict multi-tenant style isolation model based on user roles:

### 1. Admin Role (Full Access)
- **User Management**: Only Admins can register new Students and Teachers.
- **Course Management**: Only Admins can create courses and assign them to Teachers.
- **Enrollment**: Only Admins can enroll students into specific courses.
- **Global Visibility**: Admins can see all courses, students, and results across the entire system.

### 2. Teacher Role (Isolated Scope)
- **Data Isolation**: Teachers can ONLY see:
  - Courses explicitly assigned to them by an Admin.
  - Students enrolled in their courses.
  - Exams and Sessions related to their courses.
- **Management Privileges**: Teachers can create and manage Exams/Questions within their courses, extend sessions, and provide feedback.
- **Restrictions**: Teachers CANNOT create courses, register users, or see data from courses they do not own.

### 3. Student Role (Personal Scope)
- **Course Enrollment**: Students only see exams for courses they are enrolled in.
- **Signup**: Students can sign up publicly, but will have an empty dashboard until an Admin enrolls them in a course.
- **Self-Service**: Students can view their own profile, take exams they are assigned to, and view their own results.

---

## Recent Features & Stability (Late April 2026)

This update adds advanced session management, exam cloning, and result exporting features.

### 1. Advanced Session Management
- **Teacher Feedback**: Teachers can now leave comments on student exam attempts.
  - `PATCH /sessions/{sessionId}/feedback`
- **Session Extensions**: Teachers can extend an active session's time limit.
  - `POST /sessions/{sessionId}/extend`
- **Individual Attempt Overrides**: Teachers can grant additional exam attempts to specific students.
  - `POST /exams/{examId}/students/{studentId}/additional-attempts?count=1`
- **Session Heartbeat**: Students can send heartbeats to track session activity.
  - `POST /sessions/{sessionId}/heartbeat`

### 2. Exam Utility Features
- **Exam Cloning**: Teachers can duplicate an entire exam structure, including all questions and answer keys.
  - `POST /exams/{examId}/clone`
- **Published Exam Editing**: Relaxed constraints for published exams. Teachers can now update non-critical fields (title, description, visibilityMode, maxAttempts) without unpublishing.

### 3. Result Exporting & Detailed Grading
- **Detailed Status**: Results now include a `status` field: `CORRECT`, `INCORRECT`, `PARTIAL`, or `NOT_ATTEMPTED`.
- **CSV Export**: Teachers and Students can export results as a CSV file.
  - `GET /results/session/{sessionId}/export` (Returns `text/csv`)

### 4. Security & Privacy Hardening
- **Credential Masking**: User password hashes and history hashes are strictly excluded from all API JSON responses using `@JsonIgnore`.
- **Lazy Loading Fixes**: Resolved Hibernate "No Session" errors during JSON serialization of user-linked collections.

### Updated Endpoint Map

| Method | Path | What it does | Access |
|---|---|---|---|
| `PATCH` | `/sessions/{sessionId}/feedback` | Adds teacher feedback to a session. | `TEACHER` or `ADMIN` |
| `POST` | `/sessions/{sessionId}/extend` | Extends session duration (re-provisions sandbox if expired). | `TEACHER` or `ADMIN` |
| `POST` | `/sessions/{sessionId}/heartbeat` | Updates the last seen timestamp for a session. | Owner/`TEACHER`/`ADMIN` |
| `POST` | `/exams/{examId}/students/{studentId}/additional-attempts` | Grants extra attempts to a specific student. | `TEACHER` or `ADMIN` |
| `GET` | `/results/session/{sessionId}/export` | Exports student results for a session as CSV. | Owner/`TEACHER`/`ADMIN` |
| `POST` | `/exams/{examId}/clone` | Clones an existing exam (including questions/keys). | `TEACHER` or `ADMIN` |



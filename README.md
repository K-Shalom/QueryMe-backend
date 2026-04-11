# QueryMe Backend

Spring Boot backend for QueryMe, a web-based SQL exam platform for database courses.

The backend is organized as a modular monolith around the intended project flow:

- teachers create exams and questions
- each student attempt gets a private PostgreSQL schema
- student SQL is graded by result-set comparison, not query-string matching
- results are exposed according to the exam visibility mode

This README is backend-focused and grouped by the team/module ownership described in the project brief.

## Architecture Summary

- Backend: Spring Boot modular monolith
- Auth: Spring Security + JWT
- App data source: `spring.datasource`
- Sandbox data source: `queryme.sandbox-datasource.*`
- Roles in code: `TEACHER`, `STUDENT`, `ADMIN`, `GUEST`
- Server port: `8084`

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

## Alignment Highlights

These backend changes now match the intended QueryMe flow more closely:

- students only see published exams assigned to them by course or enrollment
- student-facing exam responses no longer expose `seedSql`
- student-facing question responses no longer expose `referenceQuery`
- question create and update both regenerate the answer key immediately
- sessions now enforce `maxAttempts`
- starting a session provisions the sandbox automatically
- session expiry is auto-submitted on a scheduler and tears the sandbox down
- query submissions are tied to a session, not just an exam/student pair
- query submit responses only include marks/result sets when the exam visibility mode allows it
- student result views are built from the latest submission per question for that session
- manual sandbox provisioning now returns the real configured connection info instead of a hard-coded DB user

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
- Public endpoints in the current config are:
  - `POST /api/auth/**`
  - `POST /api/teachers/register`
  - `POST /api/admins/register`
  - `POST /api/guests/register`
  - `GET /api/courses`
  - `GET /api/class-groups/**`
- Teacher/admin writes are enforced for exam mutation and course enrollment endpoints.
- Students can start and submit their own sessions, but exam-wide session listings are teacher/admin only.
- Manual sandbox endpoints are teacher/admin only.

Authorization header format:

```text
Authorization: Bearer <jwt>
```

## Group J - Auth

Base path: `/api/auth`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/auth/signup` | Registers a user with shared auth credentials. Supports `email`, `password`, `fullName`, `role`, plus optional student fields. | Public |
| `POST` | `/api/auth/signin` | Authenticates a user and returns the JWT payload. | Public |

Notes:

- Signup creates credentials in the shared `users` auth table.
- For `STUDENT` and `TEACHER`, signup also creates the matching profile record used by the other modules.

## Group F - User and Student Management

These endpoints own profile records, course structure, and teacher-driven student assignment.

### Student and Teacher Profiles

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/teachers/register` | Creates a teacher profile. | Public |
| `PUT` | `/api/teachers/{id}` | Updates a teacher profile. | `TEACHER` |
| `GET` | `/api/teachers` | Lists teachers. | `TEACHER` |
| `POST` | `/api/students/register` | Creates a student profile. | `TEACHER` or `ADMIN` |
| `PUT` | `/api/students/{id}` | Updates student profile fields such as name, password, course, and class group. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/students` | Lists students. | `TEACHER` or `ADMIN` |
| `GET` | `/api/students/{id}` | Fetches one student profile by numeric student-table id. | Authenticated |

### Courses, Class Groups, and Enrollment

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/courses` | Creates a course linked to the logged-in teacher. | `TEACHER` |
| `GET` | `/api/courses` | Lists all courses. | Public |
| `POST` | `/api/class-groups` | Creates a class group. | `TEACHER` |
| `GET` | `/api/class-groups` | Lists all class groups. | Public |
| `GET` | `/api/class-groups/course/{courseId}` | Lists class groups for one course. | Public |
| `POST` | `/api/course-enrollments` | Enrolls a student into a course. | `TEACHER` or `ADMIN` |
| `GET` | `/api/course-enrollments` | Lists all enrollments. | `TEACHER` or `ADMIN` |
| `GET` | `/api/course-enrollments/course/{courseId}` | Lists enrollments for one course. | `TEACHER` or `ADMIN` |
| `GET` | `/api/course-enrollments/student/{studentId}` | Lists enrollments for one student. | `TEACHER` or `ADMIN` |
| `DELETE` | `/api/course-enrollments` | Removes an enrollment. | `TEACHER` or `ADMIN` |

### Additional Account Endpoints Present in the Codebase

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/admins/register` | Creates an admin profile. | Public |
| `PUT` | `/api/admins/{id}` | Updates an admin profile. | `ADMIN` |
| `GET` | `/api/admins` | Lists admins. | `ADMIN` |
| `POST` | `/api/guests/register` | Creates a guest profile. | Public |
| `PUT` | `/api/guests/{id}` | Updates a guest profile. | `GUEST` |
| `GET` | `/api/guests` | Lists guests. | `GUEST` |

Notes:

- Course assignment now matters to the exam flow: student exam visibility is filtered by direct course linkage and course enrollments.
- Credentials live in the shared auth model; the profile modules own the rest of the identity data.

## Group A - Exam Module

Base path: `/api/exams`

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
| `POST` | `/api/exams` | Creates a draft exam. | `TEACHER` or `ADMIN` |
| `GET` | `/api/exams/{examId}` | Fetches one exam. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/exams/course/{courseId}` | Lists exams for one course. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/exams/published` | Lists published exams visible to the current user. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `PUT` | `/api/exams/{examId}` | Updates a draft exam. | `TEACHER` or `ADMIN` |
| `PATCH` | `/api/exams/{examId}/publish` | Publishes a draft exam. | `TEACHER` or `ADMIN` |
| `PATCH` | `/api/exams/{examId}/unpublish` | Moves a published exam back to draft. | `TEACHER` or `ADMIN` |
| `PATCH` | `/api/exams/{examId}/close` | Closes a published exam. | `TEACHER` or `ADMIN` |
| `DELETE` | `/api/exams/{examId}` | Deletes a draft exam. | `TEACHER` or `ADMIN` |

Notes:

- Students only receive exams that are both `PUBLISHED` and assigned to them.
- Student-facing `ExamResponse` values hide `seedSql`.
- `maxAttempts` now defaults to `1` if omitted.
- Only `DRAFT` exams can be edited or deleted.
- Publishing requires both `seedSql` and `visibilityMode`.

## Group I - Question Module

Base path: `/api/exams/{examId}/questions`

When a teacher creates or updates a question, the service:

1. saves the question
2. provisions a temporary sandbox seeded from the exam dataset
3. runs the teacher's `referenceQuery`
4. stores the normalized answer key JSON
5. tears the sandbox down

`QuestionRequest` supports:

- `prompt`
- `referenceQuery`
- `marks`
- `orderIndex`
- `orderSensitive`
- `partialMarks`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/exams/{examId}/questions` | Creates a question and generates the answer key immediately. | `TEACHER` or `ADMIN` |
| `PUT` | `/api/exams/{examId}/questions/{questionId}` | Updates a question and regenerates the answer key immediately. | `TEACHER` or `ADMIN` |
| `GET` | `/api/exams/{examId}/questions` | Lists questions ordered by `orderIndex`. | `STUDENT`, `TEACHER`, or `ADMIN` |

Notes:

- Students can only fetch questions for exams that are published and assigned to them.
- Student-facing question payloads hide `referenceQuery`.
- If answer-key generation fails, question creation or update fails as well.

## Group D - Sandbox Module

The runtime exam attempt flow is split into session endpoints and direct sandbox support endpoints.

### Session Lifecycle

Base path: `/api/sessions`

`StartSessionRequest` supports:

- `examId`
- `studentId`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/sessions/start` | Starts a session, validates assignment, provisions the sandbox, and stores the real schema name. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `PATCH` | `/api/sessions/{sessionId}/submit` | Submits the session and tears the sandbox down. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/sessions/{sessionId}` | Returns one session. Students are limited to their own sessions. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/sessions/student/{studentId}` | Lists sessions for one student. Students are limited to their own sessions. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/sessions/exam/{examId}` | Lists sessions for one exam. | `TEACHER` or `ADMIN` |

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

Base path: `/api/sandboxes`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/sandboxes/provision` | Provisions or reuses a sandbox schema and seeds it with the supplied SQL. | `TEACHER` or `ADMIN` |
| `GET` | `/api/sandboxes/{examId}/students/{studentId}` | Returns sandbox connection details for an active schema. | `TEACHER` or `ADMIN` |
| `DELETE` | `/api/sandboxes/{examId}/students/{studentId}` | Drops the sandbox schema and marks it dropped in the registry. | `TEACHER` or `ADMIN` |

Notes:

- The normal student flow should use `/api/sessions/start`, not manual sandbox provisioning.
- Sandbox schema names now follow the exam/student identity pattern instead of relying on a local README-only sequence.
- Sandbox connection info comes from the configured sandbox data source.

## Group G - Query Engine

Base path: `/api/query`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/query/submit` | Grades one student SQL submission against the answer key. | `STUDENT`, `TEACHER`, or `ADMIN` |

`SubmissionRequest` supports:

- `sessionId` optional
- `examId`
- `questionId`
- `studentId`
- `query`

What happens during grading:

1. the service validates that the question belongs to the supplied exam
2. the active exam session is resolved and validated
3. the SQL is checked against the blocklist
4. the student's sandbox is resolved
5. the SQL runs with a hard 10-second timeout
6. the result set is compared to the stored answer key
7. a submission row is saved with score, correctness, and captured result-set JSON
8. the results module is notified so the `results` table stays synchronized

Current scoring behavior:

- exact match: full marks
- `partialMarks = true` and matching row count: half marks
- blocklist failure, timeout, or execution failure: returned as `executionError`

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

## Group C - Results Module

Base path: `/api/results`

| Method | Path | What it does | Access |
|---|---|---|---|
| `GET` | `/api/results/session/{sessionId}` | Returns the student-facing result view for a single session. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/results/exam/{examId}/dashboard` | Returns the teacher dashboard rows for an exam. | `TEACHER` or `ADMIN` |

### Student Result View

`GET /api/results/session/{sessionId}` now returns `StudentExamResultDto`, not raw `Result` entities.

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

`GET /api/results/exam/{examId}/dashboard` returns one row per latest student submission per question:

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

- `GET /api/students/{id}` is still a general authenticated read endpoint in the current controller layer
- partial marking is intentionally simple right now: half marks when row count matches and `partialMarks` is enabled
- there is still no dedicated backend feature for revealing teacher reference queries to students after an exam, which matches the project brief's nice-to-have scope rather than its must-have scope

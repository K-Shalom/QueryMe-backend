# QueryMe Backend

Spring Boot backend for the QueryMe SQL exam platform.

This README is a codebase-driven API map: the endpoints below were skimmed from the current controllers and grouped by the team/module responsible.

## Run Locally

Start the app with:

```bash
./mvnw spring-boot:run
```

Current server settings from `application.yml`:

- Base URL: `http://localhost:8084`
- Config source: `.env` is loaded with `spring.config.import`
- Required env vars: `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- Optional env vars: `JWT_SECRET`, `JWT_EXPIRATION`

Sandbox prerequisite:

```sql
CREATE SEQUENCE IF NOT EXISTS sandbox_schema_seq START 1;
```

## Group Ownership Map

| Group | Module | What they do |
|---|---|---|
| Group J | Auth | Handles login, registration, JWT tokens, and access control. Every request flows through their security filter. |
| Group F | User and student management | Manages student and teacher profiles, course grouping, and teacher-driven student registration. Credentials still live in the shared `users` auth model. |
| Group A | Exam module | Lets teachers configure exams: title, description, timers, visibility, attempt limits, publishing, and closing. |
| Group I | Question module | Manages questions inside an exam and generates the answer key immediately from the teacher's reference query. |
| Group D | Sandbox module | Owns private PostgreSQL schemas used during exam attempts and the APIs that provision, inspect, and tear them down. |
| Group G | Query engine | Validates student SQL, runs it inside the sandbox with a timeout, compares results to the answer key, and scores the submission. |
| Group C | Results module | Aggregates scores and applies visibility rules to decide what students and teachers can see. |

## Security Snapshot

- JWT auth is enforced by `JwtAuthFilter`.
- Public endpoints in the current `SecurityConfig` are:
  - `POST /api/auth/**`
  - `POST /api/teachers/register`
  - `POST /api/admins/register`
  - `POST /api/guests/register`
  - `GET /api/courses`
  - `GET /api/class-groups/**`
- Everything else requires authentication unless stated otherwise below.
- `Authorization` header format:

```text
Authorization: Bearer <jwt>
```

Important current-code notes:

- The old `/api/users/**` endpoints documented previously are not implemented in the current project.
- Method-level security is enabled, so `@PreAuthorize` rules on controllers are active.

## Group J - Auth

Base path: `/api/auth`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/auth/signup` | Registers a user. Body supports `email`, `password`, `fullName`, `role`, plus optional `student_number` and `department`. Defaults to `STUDENT` if `role` is omitted. | Public |
| `POST` | `/api/auth/signin` | Authenticates with `email` and `password`, then returns JWT payload: `token`, `type`, `id`, `email`, `name`, `roles`. | Public |

Notes:

- `signup` creates auth credentials in the shared `users` table.
- For `STUDENT` and `TEACHER`, signup also delegates to the profile services so the matching student/teacher record is created.

## Group F - User and Student Management

These endpoints own profile records, course structure, and enrollment relationships.

### Student and Teacher Profiles

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/teachers/register` | Creates a teacher profile. Body: `email`, `password`, `fullName`, `department`. | Public |
| `PUT` | `/api/teachers/{id}` | Updates teacher profile fields such as `fullName`, `department`, and `password`. | `TEACHER` |
| `GET` | `/api/teachers` | Lists all teachers. | `TEACHER` |
| `POST` | `/api/students/register` | Creates a student profile. Body: `email`, `password`, `fullName`, optional `courseId`, `classGroupId`, `student_number`. | `TEACHER` or `ADMIN` |
| `PUT` | `/api/students/{id}` | Updates student fields such as `fullName`, `student_number`, `password`, `courseId`, and `classGroupId`. | `STUDENT`, `TEACHER`, or `ADMIN` |
| `GET` | `/api/students` | Lists all students. | `TEACHER` or `ADMIN` |
| `GET` | `/api/students/{id}` | Fetches one student by numeric id. | Any authenticated user in current config |

### Courses, Class Groups, and Enrollment

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/courses` | Creates a course linked to the currently logged-in teacher. Body is the `Course` entity, mainly `name` and `code`. | `TEACHER` |
| `GET` | `/api/courses` | Lists all courses. | Public |
| `POST` | `/api/class-groups` | Creates a class group. Body is the `ClassGroup` entity, mainly `name` plus its `course`. | `TEACHER` |
| `GET` | `/api/class-groups` | Lists all class groups. | Public |
| `GET` | `/api/class-groups/course/{courseId}` | Lists class groups for one course. | Public |
| `POST` | `/api/course-enrollments` | Enrolls a student into a course. Body: `course_id`, `student_id`. | Any authenticated user in current config |
| `GET` | `/api/course-enrollments` | Lists all enrollments. | Any authenticated user in current config |
| `GET` | `/api/course-enrollments/course/{courseId}` | Lists enrollments for one course. | Any authenticated user in current config |
| `GET` | `/api/course-enrollments/student/{studentId}` | Lists enrollments for one student. | Any authenticated user in current config |
| `DELETE` | `/api/course-enrollments` | Removes an enrollment. Body: `course_id`, `student_id`. | Any authenticated user in current config |

### Additional Account Endpoints Present in the Codebase

These sit slightly outside the ownership summary above, but they exist in the current controllers and are closest to the same profile/account area.

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/admins/register` | Creates an admin profile. Body: `email`, `password`, `fullName`. | Public |
| `PUT` | `/api/admins/{id}` | Updates admin profile fields such as `fullName` and `password`. | `ADMIN` |
| `GET` | `/api/admins` | Lists admins. | `ADMIN` |
| `POST` | `/api/guests/register` | Creates a guest profile. Body: `email`, `password`, `fullName`. | Public |
| `PUT` | `/api/guests/{id}` | Updates guest profile fields such as `fullName` and `password`. | `GUEST` |
| `GET` | `/api/guests` | Lists guests. | `GUEST` |

## Group A - Exam Module

Base path: `/api/exams`

Exam lifecycle in the current service:

```text
DRAFT -> PUBLISHED -> CLOSED
  ^        |
  |        v
  +---- UNPUBLISH
```

`CreateExamRequest` fields:

- `courseId`
- `title`
- `description`
- `visibilityMode` as `IMMEDIATE`, `END_OF_EXAM`, or `NEVER`
- `timeLimitMins`
- `maxAttempts`
- `seedSql`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/exams` | Creates a draft exam. `seedSql` and `visibilityMode` are required by the service. | Any authenticated user in current config |
| `GET` | `/api/exams/{examId}` | Fetches one exam. | Any authenticated user in current config |
| `GET` | `/api/exams/course/{courseId}` | Lists exams for one course. | Any authenticated user in current config |
| `GET` | `/api/exams/published` | Lists exams with `status = PUBLISHED`. | Any authenticated user in current config |
| `PUT` | `/api/exams/{examId}` | Updates a draft exam. Only `DRAFT` exams can be edited. | Any authenticated user in current config |
| `PATCH` | `/api/exams/{examId}/publish` | Publishes a draft exam. Requires `seedSql` and `visibilityMode`. | Any authenticated user in current config |
| `PATCH` | `/api/exams/{examId}/unpublish` | Moves a published exam back to `DRAFT`. | Any authenticated user in current config |
| `PATCH` | `/api/exams/{examId}/close` | Closes a published exam. | Any authenticated user in current config |
| `DELETE` | `/api/exams/{examId}` | Deletes a draft exam. | Any authenticated user in current config |

Notes:

- `ExamResponse` currently returns `seedSql`, so callers receive the raw exam dataset seed.
- The service enforces business rules around `DRAFT`, `PUBLISHED`, and `CLOSED`, even where route-level role checks are broad.

## Group I - Question Module

Base path: `/api/exams/{examId}/questions`

When a teacher adds a question, the service immediately:

1. Saves the question.
2. Provisions a temporary sandbox using the exam's `seedSql`.
3. Runs the teacher's `referenceQuery`.
4. Stores the answer key JSON.
5. Tears the sandbox down.

`QuestionRequest` fields:

- `prompt`
- `referenceQuery`
- `marks`
- `orderIndex`
- `orderSensitive` default `false`
- `partialMarks` default `false`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/exams/{examId}/questions` | Creates a question and generates the answer key immediately from `referenceQuery`. | Authenticated; controller also declares `TEACHER` or `ADMIN` intent with `@PreAuthorize` |
| `GET` | `/api/exams/{examId}/questions` | Lists questions ordered by `orderIndex`. | Authenticated; controller also declares `TEACHER`, `STUDENT`, or `ADMIN` intent with `@PreAuthorize` |

Notes:

- `QuestionResponse` currently includes `referenceQuery`, so student-facing clients should handle that carefully.
- If answer-key generation fails, the question creation transaction fails too.

## Group D - Sandbox Module

In the current codebase, the exam runtime is split into two API surfaces:

- `/api/sessions` manages attempt/session records.
- `/api/sandboxes` manages the actual PostgreSQL schemas.

### Session Lifecycle Endpoints

Base path: `/api/sessions`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/sessions/start` | Starts a session for `examId` and `studentId`. Exam must already be `PUBLISHED`. Prevents duplicate session creation for the same student and exam. | Any authenticated user in current config |
| `PATCH` | `/api/sessions/{sessionId}/submit` | Marks a session as submitted. Rejects double submission and rejects late submission after expiry. | Any authenticated user in current config |
| `GET` | `/api/sessions/{sessionId}` | Returns one session. | Any authenticated user in current config |
| `GET` | `/api/sessions/exam/{examId}` | Lists sessions for one exam. | Any authenticated user in current config |
| `GET` | `/api/sessions/student/{studentId}` | Lists sessions for one student. | Any authenticated user in current config |

`StartSessionRequest` fields:

- `examId`
- `studentId`

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

### Sandbox Provisioning Endpoints

Base path: `/api/sandboxes`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/sandboxes/provision` | Creates or reuses the sandbox schema for one `examId` and `studentId`, then seeds it with `seedSql`. | Any authenticated user in current config |
| `GET` | `/api/sandboxes/{examId}/students/{studentId}` | Returns active sandbox connection details. | Any authenticated user in current config |
| `DELETE` | `/api/sandboxes/{examId}/students/{studentId}` | Drops the sandbox schema and marks it dropped in the registry. | Any authenticated user in current config |

`SandboxProvisionRequest` fields:

- `examId`
- `studentId`
- `seedSql`

Important implementation note:

- Starting a session now provisions the student's sandbox automatically and stores the real schema name returned by `SandboxService`.
- Submitting a session tears the sandbox down automatically. The `/api/sandboxes/**` endpoints are still available for direct lifecycle management when needed.

## Group G - Query Engine

Base path: `/api/query`

| Method | Path | What it does | Access |
|---|---|---|---|
| `POST` | `/api/query/submit` | Grades one student SQL submission. Body: `examId`, `questionId`, `studentId`, `query`. | Any authenticated user in current config |

What happens during grading:

1. The query is validated against a destructive-SQL blocklist.
2. The student's sandbox is resolved from Group D.
3. SQL is executed with a hard 10-second timeout.
4. The result set is normalized and compared to the stored answer key.
5. The submission is saved with `isCorrect`, `score`, and optional `executionError`.
6. A `Result` record is created or refreshed for that submission so the results module can expose it immediately.

Scoring behavior in the current service:

- Exact match: full marks
- Partial marks enabled and row count matches: half marks
- Validation failure, timeout, or execution failure: `executionError` is returned and the submission is still saved

## Group C - Results Module

Base path: `/api/results`

| Method | Path | What it does | Access |
|---|---|---|---|
| `GET` | `/api/results/session/{sessionId}` | Returns session results visible to a student. | Any authenticated user in current config |
| `GET` | `/api/results/exam/{examId}/dashboard` | Returns teacher dashboard results for an exam. | Authenticated; controller also declares `TEACHER` intent with `@PreAuthorize` |

Current behavior:

- Student result visibility is now driven by the real `Exam.visibilityMode`.
- `IMMEDIATE` shows results as soon as submissions are graded.
- `END_OF_EXAM` hides results until the exam is moved to `CLOSED`.
- `NEVER` returns no student-visible results.
- Query submissions now flow into the `results` table automatically through `processNewSubmission(...)`.

## Runtime Notes

- The app runs on `http://localhost:8084`.
- Method-level security is enabled through `@EnableMethodSecurity`.
- Sandbox cleanup scheduling is enabled through `@EnableScheduling`.
- The older `/api/users/**` and `/api/users/me` docs were removed because those controllers do not exist in the current project.

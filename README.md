
# QueryMe Backend

---

## Table of Contents

- [Project Setup](#project-setup)
- [Group J — Auth Module](#group-j--auth-module)
- [Group A — Exam Module](#group-a--exam-module)
- [Group I — Question Module](#group-i--question-module)
- [Group D — Sandbox Environment Module](#group-d--sandbox-environment-module)
- [Group G — Query Engine Module](#group-g--query-engine-module)

---

### Environment Variables

```bash
# Optional — override default admin seeded on startup
ADMIN_EMAIL=admin@gmail.com 
ADMIN_PASSWORD=Admin@1234
```

### Run the server

```bash
./mvnw spring-boot:run
```

Server starts on **`http://localhost:8080`**

> On first startup, a default ADMIN account is automatically created using `ADMIN_EMAIL` and `ADMIN_PASSWORD`.
> Change these before deploying to any shared environment.

---

# Group J — Auth Module

Every HTTP request on the platform passes through the JWT filter maintained by this group.
This section documents how to authenticate and how other groups should handle tokens.

## Database Schema

Roles are **NOT** a separate table. They are stored as a `varchar` column directly
on the `users` table — one table, no join table required.

```sql
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,   -- 'ADMIN' | 'TEACHER' | 'STUDENT' | 'GUEST'
    created_at    TIMESTAMP    DEFAULT now()
);
```

## Roles Reference

| Role | Description |
|---|---|
| `ADMIN` | Full access — manage users, platform-wide visibility |
| `TEACHER` | Create and manage exams, view all student results |
| `STUDENT` | Participate in assigned exams, view own results |
| `GUEST` | Read-only, limited access |

## Base URL

```text
http://localhost:8080/api/auth
http://localhost:8080/api/users
```

## Auth Endpoints (No token required)

### Register a new user

```text
POST /api/auth/signup
Content-Type: application/json
```

Request body:

```json
{
  "email": "alice@gmail.com",
  "password": "Secret@99",
  "role": "STUDENT"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | yes | Must be unique |
| `password` | string | yes | Min 6, max 40 characters |
| `role` | string | no | `ADMIN`, `TEACHER`, `STUDENT`, `GUEST` — defaults to `STUDENT` |

**Response `200 OK`:**

```json
{
  "message": "User registered successfully!"
}
```

**Error — email already in use:**

```json
{
  "message": "Error: Email is already in use!"
}
```

**Error — invalid role:**

```json
{
  "message": "Error: Invalid role 'XYZ'. Valid values: ADMIN, TEACHER, STUDENT, GUEST"
}
```

---

### Login

```text
POST /api/auth/signin
Content-Type: application/json
```

Request body:

```json
{
  "email": "alice@gmail.com",
  "password": "Secret@99"
}
```

**Response `200 OK`:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alice@gmail.com",
  "roles": ["STUDENT"]
}
```

> Save the `token` value — you must send it in every subsequent request.

---

## User Endpoints (Token required)

Include the token in every request:

```text
Authorization: Bearer <your_token_here>
```

---

### Get own profile

```text
GET /api/users/me
Authorization: Bearer <token>
```

Available to **any authenticated role**.

**Response `200 OK`:**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alice@gmail.com",
  "role": "STUDENT",
  "createdAt": "2026-04-02T21:26:38"
}
```

---

### List all users *(ADMIN only)*

```text
GET /api/users
Authorization: Bearer <admin_token>
```

**Response `200 OK`:** array of user objects

---

### Filter users by role *(ADMIN or TEACHER)*

```text
GET /api/users/role/{role}
Authorization: Bearer <admin_or_teacher_token>
```

Example:

```text
GET /api/users/role/STUDENT
```

**Response `200 OK`:** array of users with that role

---

### Get user by ID *(ADMIN only)*

```text
GET /api/users/{id}
Authorization: Bearer <admin_token>
```

**Response `200 OK`:** single user object, or `404 Not Found`

---

### Delete user *(ADMIN only)*

```text
DELETE /api/users/{id}
Authorization: Bearer <admin_token>
```

**Response `200 OK`:**

```json
"User deleted successfully."
```

---

## Route Security Reference

All routes outside `/api/auth/**` require a valid JWT. Unauthorized requests return `401`.
Requests with a valid JWT but insufficient role return `403`.

| Route pattern | Minimum role |
|---|---|
| `POST /api/auth/**` | Public |
| `GET /api/users/me` | Any authenticated user |
| `GET/DELETE /api/users/**` | `ADMIN` |
| `GET /api/users/role/**` | `ADMIN` or `TEACHER` |
| `/api/exams/**` | `TEACHER` or `ADMIN` |
| `/api/questions/**` | `TEACHER` or `ADMIN` |
| `/api/submissions/**` | `STUDENT`, `TEACHER`, or `ADMIN` |
| `/api/results/**` | `STUDENT`, `TEACHER`, or `ADMIN` |

## How to Integrate (For Other Groups)

### Protect your endpoints with `@PreAuthorize`

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    // Only teachers and admins can create exams
    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN')")
    public ResponseEntity<?> createExam(...) { ... }

    // Students, teachers, and admins can view published exams
    @GetMapping("/published")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<?> getPublishedExams() { ... }
}
```

### Get the current user's details inside a service

```java
import com.year2.queryme.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Inside any @Service or @RestController method:
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
UserDetailsImpl currentUser = (UserDetailsImpl) auth.getPrincipal();

UUID userId  = currentUser.getId();
String email = currentUser.getEmail();
String role  = currentUser.getAuthorities().iterator().next().getAuthority(); // e.g. "STUDENT"
```

### Quick Postman test flow

1. `POST /api/auth/signup` — create a STUDENT, a TEACHER, and use the default ADMIN
2. `POST /api/auth/signin` — login with each; copy the returned `token`
3. Set **Authorization → Bearer Token** in Postman using the copied token
4. Call protected endpoints and verify the correct `200` / `403` responses per role

---

# Group A — Exam Module

**Base URL:** `http://localhost:8080/api/exams`

All endpoints require a valid JWT token:

```text
Authorization: Bearer <token>
```

## Exam Status Lifecycle

```text
DRAFT → PUBLISHED → CLOSED
  ↑________↓
 (unpublish)
```

## Endpoints

### Create an exam

```text
POST /api/exams
```

**Request body:**

```json
{
  "courseId": "uuid-of-course",
  "title": "Database CAT 1",
  "description": "Optional description",
  "visibilityMode": "IMMEDIATE",
  "timeLimitMins": 60,
  "maxAttempts": 1,
  "seedSql": "CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), grade INT); INSERT INTO students VALUES (1, 'Alice', 85);"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `courseId` | string | yes | UUID from Group F |
| `title` | string | yes | |
| `description` | string | no | |
| `visibilityMode` | string | yes | `IMMEDIATE`, `END_OF_EXAM`, `NEVER` |
| `timeLimitMins` | integer | no | `null` = no time limit |
| `maxAttempts` | integer | no | Defaults to `1` |
| `seedSql` | string | yes | SQL for sandbox seeding — Group D uses this |

**Response `200 OK`:**

```json
{
  "id": "36b28504-1902-4ac4-89a1-1271f0ead90e",
  "courseId": "uuid-of-course",
  "title": "Database CAT 1",
  "description": "Optional description",
  "status": "DRAFT",
  "visibilityMode": "IMMEDIATE",
  "timeLimitMins": 60,
  "maxAttempts": 1,
  "seedSql": "CREATE TABLE...",
  "createdAt": "2026-04-01T11:16:10.448",
  "publishedAt": null
}
```

---

### Get exam by ID

```text
GET /api/exams/{examId}
```

**Response `200 OK`:** full exam object

---

### Get exams by course

```text
GET /api/exams/course/{courseId}
```

**Response `200 OK`:** array of exam objects for that course

---

### Get all published exams

```text
GET /api/exams/published
```

**Response `200 OK`:** array of exams where `status = PUBLISHED`

---

### Update an exam

```text
PUT /api/exams/{examId}
```

> Only allowed when `status = DRAFT`

**Request body** (all fields optional):

```json
{
  "title": "Updated title",
  "description": "Updated description",
  "visibilityMode": "END_OF_EXAM",
  "timeLimitMins": 90,
  "maxAttempts": 2,
  "seedSql": "CREATE TABLE..."
}
```

**Response `200 OK`:** updated exam object

**Error if not DRAFT:**

```json
{ "message": "Only DRAFT exams can be edited" }
```

---

### Publish an exam

```text
PATCH /api/exams/{examId}/publish
```

Validation before publish:
- Status must be `DRAFT`
- `seedSql` must not be empty
- `visibilityMode` must be set

**Response `200 OK`:**

```json
{
  "status": "PUBLISHED",
  "publishedAt": "2026-04-01T11:20:00.000"
}
```

---

### Unpublish an exam

```text
PATCH /api/exams/{examId}/unpublish
```

> Moves `PUBLISHED` back to `DRAFT`

**Response `200 OK`:**

```json
{
  "status": "DRAFT",
  "publishedAt": null
}
```

**Error if not PUBLISHED:**

```json
{ "message": "Only PUBLISHED exams can be unpublished" }
```

---

### Close an exam

```text
PATCH /api/exams/{examId}/close
```

> Moves `PUBLISHED` to `CLOSED`. Cannot be reversed.

**Error if not PUBLISHED:**

```json
{ "message": "Only PUBLISHED exams can be closed" }
```

---

### Delete an exam

```text
DELETE /api/exams/{examId}
```

> Only `DRAFT` exams can be deleted

**Response `204 No Content`**

**Error if not DRAFT:**

```json
{ "message": "Only DRAFT exams can be deleted" }
```

---

## Visibility Mode Reference

| Value | Meaning |
|---|---|
| `IMMEDIATE` | Student sees results right after submitting |
| `END_OF_EXAM` | Student sees results only after exam is closed |
| `NEVER` | Results never shown to student |

---

## Error Reference

| Status | Meaning |
|---|---|
| `200 OK` | Success |
| `204 No Content` | Delete succeeded |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | Valid token but insufficient role |
| `500` | Business rule violation — check `message` field |

---

# Group I — Question Module

**Overview:** This module manages the creation and retrieval of questions for a specific exam. Its most critical responsibility is **Answer Key Generation**. When a teacher saves a question, this module automatically communicates with the Sandbox (Group D) and Query Engine (Group G) to run the teacher's reference query against the exam's seed data. It captures the resulting data and saves it as a normalized JSON `AnswerKey` for automated grading later.

## Base URL
```text
http://localhost:8080/api/exams/{examId}/questions
```

All endpoints require a valid JWT token in the header:
```text
Authorization: Bearer <token>
```

## Endpoints

### 1. Create a Question (and Generate Answer Key)

```text
POST /api/exams/{examId}/questions
```
> **Requires Role:** `TEACHER` or `ADMIN`

This endpoint saves the question details and triggers the automated Answer Key generation process. It will provision a temporary sandbox, execute the `referenceQuery`, store the JSON output, and tear down the sandbox.

**Request Body:**
```json
{
  "prompt": "Write a query to select all columns from the users table where the age is greater than 21.",
  "referenceQuery": "SELECT * FROM users WHERE age > 21;",
  "marks": 10,
  "orderIndex": 1,
  "orderSensitive": false,
  "partialMarks": false
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `prompt` | string | yes | The text displayed to the student |
| `referenceQuery` | string | yes | The correct SQL query used to generate the answer key |
| `marks` | integer | yes | Total points awarded for a correct answer |
| `orderIndex` | integer | yes | The display order of the question in the exam |
| `orderSensitive` | boolean | no | Defaults to `false`. If true, student rows must match exactly in order |
| `partialMarks` | boolean | no | Defaults to `false`. Allows 50% credit for row-count matches |

**Response `201 Created`:**
```json
{
  "id": "e8aaee82-f787-4fab-93fa-6fbc1a1e8530",
  "examId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "prompt": "Write a query to select all columns from the users table where the age is greater than 21.",
  "referenceQuery": "SELECT * FROM users WHERE age > 21;",
  "marks": 10,
  "orderIndex": 1,
  "orderSensitive": false,
  "partialMarks": false
}
```

*Note: The `AnswerKey` is generated silently in the background and stored in the `answer_keys` table. It is intentionally NOT returned in this response to prevent accidental exposure to students.*

**Error `500 Internal Server Error`:**
If the `referenceQuery` contains invalid SQL or relies on tables not present in the Exam's `seedSql`, the sandbox will reject it, the transaction will roll back, and the question will **not** be saved.

---

### 2. Get All Questions for an Exam

```text
GET /api/exams/{examId}/questions
```
> **Requires Role:** `TEACHER`, `STUDENT`, or `ADMIN`

Retrieves a list of all questions belonging to a specific exam, ordered by their `orderIndex`.

**Response `200 OK`:**
```json
[
  {
    "id": "e8aaee82-f787-4fab-93fa-6fbc1a1e8530",
    "examId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "prompt": "Write a query to select all columns from the users table where the age is greater than 21.",
    "referenceQuery": "SELECT * FROM users WHERE age > 21;",
    "marks": 10,
    "orderIndex": 1,
    "orderSensitive": false,
    "partialMarks": false
  },
  {
    "id": "b1ccdd99-a123-4bcc-88df-9cdd2b2f9911",
    "examId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "prompt": "Count the total number of users.",
    "referenceQuery": "SELECT COUNT(*) FROM users;",
    "marks": 5,
    "orderIndex": 2,
    "orderSensitive": false,
    "partialMarks": false
  }
]
```
*(Note: If a `STUDENT` calls this endpoint, the frontend should ideally hide the `referenceQuery` from the UI unless the exam settings permit showing it).*

---

### Integrating with the Query Engine (Group G)
Group G will use the Question Module to fetch the correct Answer Key during student grading.
**Internal Service Call:**
```java
// Inject QuestionService into your grading class
AnswerKeyDto answerKey = questionService.getAnswerKeyForQuestion(questionId);
```

---

# Group D — Sandbox Environment Module

**Overview:** This module dynamically provisions and manages isolated PostgreSQL schemas
for individual student exam sessions. Each student gets a private schema seeded with the
exam dataset, so queries cannot affect other students or the application database.

## Architecture

### Security Model

- **Isolation**: Each student gets a unique schema (e.g., `exam_123_student_456`) and a dedicated database user with randomly generated credentials.
- **Blast Radius Containment**: Students are explicitly revoked access to the `public` schema and granted full CRUD permissions only on their assigned schema.
- **Automated Cleanup**: Expired sandboxes are automatically torn down to free up server resources.

### Key Components

| Class | Role |
|---|---|
| `SandboxService` | Primary interface — provision, retrieve connection info, teardown |
| `SandboxRegistry` | JPA entity tracking active sandboxes in the `sandbox_registry` table |
| `SandboxCleanupScheduler` | Runs every 5 minutes to remove expired sandboxes |

## Overview

The Sandbox Environment Module provides schema-based PostgreSQL isolation for exam execution within the QueryMe monolith. It provisions a dedicated schema for each exam and student pairing, optionally applies seed data, and records sandbox metadata for lifecycle tracking.

This module uses the shared database user `level6year2` for schema-based isolation. The shared user remains consistent across sandbox operations while isolation is achieved through dedicated PostgreSQL schemas.

## Key Features

### Validation
Before provisioning a sandbox, the service validates that both the exam and the student exist in the system. This prevents invalid or orphaned sandbox creation and keeps the workflow aligned with the monolith’s existing records.

### Isolation
Each sandbox is isolated through a unique schema derived from the exam and student identifiers. This keeps student execution separated from the rest of the application data while still operating in the shared database environment.

### 63-Character Safety
PostgreSQL identifiers are limited to 63 characters. The sandbox module applies schema naming rules that keep generated identifiers safe, normalized, and compatible with PostgreSQL limits.

## API Documentation

Base path: `http://localhost:8080/api/sandboxes`

### Endpoint Summary

| Endpoint | Method | Purpose | Input | Success Response |
|---|---|---|---|---|
| `/provision` | POST | Provision or reuse a sandbox schema for an exam/student pair | JSON body with `examId`, `studentId`, optional `seedSql` | `201 Created` with `schemaName`, `dbUsername` |
| `/{examId}/students/{studentId}` | GET | Retrieve active sandbox connection details | `examId` and `studentId` as path variables | `200 OK` with `schemaName`, `dbUsername` |
| `/{examId}/students/{studentId}` | DELETE | Tear down sandbox schema and update registry status | `examId` and `studentId` as path variables | `200 OK` with success `message` |

### How These Endpoints Work

| Step | Endpoint | What Happens Internally |
|---|---|---|
| 1 | `POST /provision` | Validates exam and student records, generates schema name, creates schema if missing, optionally executes `seedSql`, stores registry metadata, returns sandbox connection info. |
| 2 | `GET /{examId}/students/{studentId}` | Looks up sandbox registry by exam and student, confirms sandbox status is active, then returns schema and database username. |
| 3 | `DELETE /{examId}/students/{studentId}` | Finds the sandbox registry record, drops the schema, updates status, and returns a confirmation message. |

### JSON Sample Data

#### 1) Provision Sandbox

Request (`POST /api/sandboxes/provision`):

```json
{
  "examId": "7f8c2f5f-1ad2-4a8f-a4e2-81f6cb2e4d11",
  "studentId": "2c39c7f9-85f8-4b2a-90c8-d4f4b5d99f73",
  "seedSql": "CREATE TABLE IF NOT EXISTS answers (id UUID PRIMARY KEY, answer_text VARCHAR(255)); INSERT INTO answers (id, answer_text) VALUES ('9d4f8a89-7c7f-4a98-9cc5-ae9e1d6c5f10', 'Sample answer');"
}
```

Response (`201 Created`):

```json
{
  "schemaName": "exam_7f8c2f5f1ad24a8fa4e281f6cb2e4d11_student_2c39c7f985f84b2a90c8d4f4b5d99f73",
  "dbUsername": "level6year2"
}
```

#### 2) Get Sandbox Connection Details

Request (`GET /api/sandboxes/7f8c2f5f-1ad2-4a8f-a4e2-81f6cb2e4d11/students/2c39c7f9-85f8-4b2a-90c8-d4f4b5d99f73`)

Response (`200 OK`):

```json
{
  "schemaName": "exam_7f8c2f5f1ad24a8fa4e281f6cb2e4d11_student_2c39c7f985f84b2a90c8d4f4b5d99f73",
  "dbUsername": "level6year2"
}
```

#### 3) Tear Down Sandbox

Request (`DELETE /api/sandboxes/7f8c2f5f-1ad2-4a8f-a4e2-81f6cb2e4d11/students/2c39c7f9-85f8-4b2a-90c8-d4f4b5d99f73`)

Response (`200 OK`):

```json
{
  "message": "Sandbox successfully dropped for examId=7f8c2f5f-1ad2-4a8f-a4e2-81f6cb2e4d11 and studentId=2c39c7f9-85f8-4b2a-90c8-d4f4b5d99f73"
}
```

### Method Reference

| Method | Parameters | Returns | Description |
|---|---|---|---|
| `provisionSandbox` | `examId`, `studentId`, `seedSql` | `String` schemaName | Creates schema, user, seeds data |
| `getSandboxConnectionDetails` | `examId`, `studentId` | `SandboxConnectionInfo` | Returns schema + credentials |
| `teardownSandbox` | `examId`, `studentId` | `void` | Drops schema and user permanently |

> Handle exceptions for all three calls — provisioning can fail if the DB user lacks `CREATEROLE`.

---

# Group G — Query Engine Module

**Overview:** The Query Engine is the core of the QueryMe platform. It is responsible for receiving student SQL queries, validating them for security, executing them in a timed sandbox, and grading the results against an answer key.

## Technical Tasks

- **Query Validation**: Regex-based blocklist filtering to prevent destructive SQL operations.
- **Sandboxed Execution**: Hard-timeout (10s) query execution with restricted schema access.
- **Result-Set Comparison**: Order-insensitive and type-normalized comparison of student output against teacher reference keys.
- **Scoring**: Full marks for exact data matches, and optional **Partial Marks** (50%) for row-count matches.

## Endpoints

### Submit a Query
```text
POST /api/query/submit
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "examId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "questionId": "e8aaee82-f787-4fab-93fa-6fbc1a1e8530",
  "studentId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "query": "SELECT * FROM students"
}
```

**Response `200 OK`:**
```json
{
  "submissionId": "...",
  "isCorrect": true,
  "score": 10,
  "executionError": null
}
```

---

## Testing Your Implementation (Group G)

Follow these steps in Postman to verify your module is "Demo-Ready":

### 1. Test Security (Blocklist)
Submit a query like `DROP TABLE students;`.
- **Expected**: `executionError` should contain "Validation Error" and name the blocked keyword.

### 2. Test Robustness (Numeric Matching)
If the answer key has `1` but the student query returns `1.0`, our engine will still mark it as **Correct**.

### 3. Test Fairness (Order-Insensitivity)
Submit a query like `SELECT * FROM students` and ensure it matches the answer key even if the rows or columns are slightly rearranged.

### 4. Test Performance (Timeout)
Submit `SELECT pg_sleep(11);`.
- **Expected**: `executionError` should say "Timeout Error: Query exceeded 10s execution limit."

### 5. Test Partial Marks
If a question has `partialMarks: true`, try a query that returns the correct number of rows but wrong data.
- **Expected**: `score` should be **50%** of the question's marks.

---
*For issues related to the Query Engine, contact Group G.*
```
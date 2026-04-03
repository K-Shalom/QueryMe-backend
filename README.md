# Group A — Exam Module API

Base URL: `http://localhost:8080/api/exams`

All endpoints require a valid JWT token:
```
Authorization: Bearer <token>
```

---

## Exam status lifecycle

```
DRAFT → PUBLISHED → CLOSED
  ↑________↓
 (unpublish)
```

---

## Endpoints

### Create an exam
```
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
| `maxAttempts` | integer | no | defaults to `1` |
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
```
GET /api/exams/{examId}
```

**Response `200 OK`:** full exam object

---

### Get exams by course
```
GET /api/exams/course/{courseId}
```

**Response `200 OK`:** array of exam objects for that course

---

### Get all published exams
```
GET /api/exams/published
```

**Response `200 OK`:** array of exams where `status = PUBLISHED`

---

### Update an exam
```
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
```
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
  "publishedAt": "2026-04-01T11:20:00.000",
  ...
}
```

**Error if validation fails:**
```json
{ "message": "Only DRAFT exams can be published" }
```

---

### Unpublish an exam
```
PATCH /api/exams/{examId}/unpublish
```

> Moves `PUBLISHED` back to `DRAFT`

**Response `200 OK`:**
```json
{
  "status": "DRAFT",
  "publishedAt": null,
  ...
}
```

**Error if not PUBLISHED:**
```json
{ "message": "Only PUBLISHED exams can be unpublished" }
```

---

### Close an exam
```
PATCH /api/exams/{examId}/close
```

> Moves `PUBLISHED` to `CLOSED`. Cannot be reversed.

**Response `200 OK`:**
```json
{
  "status": "CLOSED",
  ...
}
```

**Error if not PUBLISHED:**
```json
{ "message": "Only PUBLISHED exams can be closed" }
```

---

### Delete an exam
```
DELETE /api/exams/{examId}
```

> Only `DRAFT` exams can be deleted

**Response `204 No Content`**

**Error if not DRAFT:**
```json
{ "message": "Only DRAFT exams can be deleted" }
```

---

## Visibility mode reference

| Value | Meaning |
|---|---|
| `IMMEDIATE` | Student sees results right after submitting |
| `END_OF_EXAM` | Student sees results only after exam is closed |
| `NEVER` | Results never shown to student |

---

## Error reference

| Status | Meaning |
|---|---|
| `200 OK` | Success |
| `204 No Content` | Delete succeeded |
| `401 Unauthorized` | Missing or invalid JWT token |
| `500` | Business rule violation — check `message` field |

---

# Group D — Sandbox Environment Module

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

### Error Samples

Validation failure example (`400/500` depending on global exception mapping):

```json
{
  "message": "Exam not found in registry"
}
```

Authorization failure example:

```json
{
  "path": "/api/sandboxes/provision",
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "status": 401
}
```


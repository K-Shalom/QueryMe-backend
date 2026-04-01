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

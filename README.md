# QueryMe Backend - Group F

This backend handles **User & Student Management**, including registration, course/class grouping, and profile management.

## Prerequisites
- **Java 17 or 21** (JDK 17 or higher)
- **MySQL Server**
- **Maven** (optional, `./mvnw` is included)

## Setup Instructions

### 1. Database Configuration
Create a new MySQL database named `UserManagement_db`:
```sql
CREATE DATABASE UserManagement_db;
```

### 2. Environment Variables
1. Copy the [.env.example](.env.example) file and rename it to `.env`.
2. Update the credentials in the `.env` file to match your MySQL setup:
   - `DB_HOST=localhost`
   - `DB_NAME=UserManagement_db`
   - `DB_USER=your_root`
   - `DB_PASSWORD=your_password`

### 3. Build the Project
Run this command in the project root to compile:
```powershell
./mvnw clean compile
```

### 4. Run the Application
Start the Spring Boot application:
```powershell
./mvnw spring-boot:run
```
The server will start at `http://localhost:8080`.

## API Endpoints (Quick Reference)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/courses` | Create a new course |
| `POST` | `/api/class-groups` | Create academic class grouping |
| `POST` | `/api/students/register` | Register a new student |
| `GET` | `/api/students` | List all students |
| `PUT` | `/api/students/{id}` | Update student profile |
| `PUT` | `/api/teachers/{id}` | Update teacher profile |

---
**Note**: The application is configured with `jpa.hibernate.ddl-auto: update`, so tables will be created automatically upon first run.

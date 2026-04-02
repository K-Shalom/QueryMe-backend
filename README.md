# 🎓 QueryMe - User & Student Management System (Group F)

QueryMe is a robust Spring Boot backend designed to handle multi-role educational management. It provides a secure, role-based architecture for managing Teachers, Students, Admins, and Guests, alongside academic structures like Courses and Class Groups.

---

## 🚀 Core Features

- **Multi-Role Authentication**: JWT-based security supporting 4 distinct roles (Admin, Teacher, Student, Guest).
- **Academic Management**: Teacher-led course creation and student registration.
- **Profile Orchestration**: Dynamic profile management for both educators and learners.
- **Resilient Database Mapping**: Built-in sanitization for legacy database compatibility (handling ID padding and binary mismatches).
- **Automated Schema Repair**: Self-healing database initialization on startup.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **Security**: Spring Security & Structured JWT (JSON Web Tokens)
- **Database**: MySQL / PostgreSQL Support
- **Persistence**: Spring Data JPA (Hibernate)
- **Tooling**: Lombok, Maven, Jakarta Validation

---

## 📂 Getting Started

### 1. Prerequisites
- **JDK 21** or higher.
- **MySQL Instance** (The application is currently configured for a remote instance at `35.238.161.33`).

### 2. Installation
Clone the repository and compile the project using the included Maven wrapper:
```powershell
./mvnw clean install
```

### 3. Running the Application
```powershell
./mvnw spring-boot:run
```
The server will be available at: `http://localhost:8080`

---

## 👤 Role-Based Guide (Postman Workflow)

### 👨‍🏫 TEACHER Flow
*Goal: Manage curriculum and register students.*
1.  **Signup / Login**: Sign in as `teacher@queryme.com` | `Teacher@123`.
2.  **Course Creation**: `POST /api/courses` to create your classroom.
3.  **Student Registration**: `POST /api/students/register` using your Teacher token.

### 👩‍🎓 STUDENT Flow
*Goal: Profile management and course interaction.*
1.  **Login**: Use credentials provided by your teacher (e.g., `alice@queryme.com` | `Student@123`).
2.  **Profile**: `GET /api/students/profile` to view your data.
3.  **Update**: `PUT /api/students/{id}` to refresh your info.

### 🛡️ ADMIN Flow
*Goal: System-wide oversight.*
1.  **Login**: Sign in as `admin@queryme.com` | `Admin@123`.
2.  **Audit**: Use `GET /api/students` to see the full register of active learners.

---


## 📜 Full API Reference

| Category | Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | `POST` | `/api/auth/signin` | Authenticate & get JWT Token | Public |
| | `POST` | `/api/auth/signup` | Self-register a new user | Public |
| **Academic** | `POST` | `/api/courses` | Create a new Course | Teacher/Admin |
| | `GET` | `/api/courses` | List all available Courses | Any |
| | `POST` | `/api/class-groups` | Create an Academic Class Group | Teacher/Admin |
| | `GET` | `/api/class-groups` | List all Class Groups | Any |
| | `GET` | `/api/class-groups/course/{id}` | Get groups for a specific Course | Any |
| **Students** | `POST` | `/api/students/register` | Register a new Student | Teacher/Admin |
| | `GET` | `/api/students` | List all Students | Admin |
| | `GET` | `/api/students/{id}` | Get specific Student details | Any |
| | `PUT` | `/api/students/{id}` | Update Student profile | Student/Admin |
| **Teachers** | `POST` | `/api/teachers/register` | Register a new Teacher | Admin |
| | `GET` | `/api/teachers` | List all Teachers | Admin |
| | `PUT` | `/api/teachers/{id}` | Update Teacher profile | Teacher/Admin |
| **Admins** | `POST` | `/api/admins/register` | Register a new Admin | Admin |
| | `GET` | `/api/admins` | List all Admins | Admin |
| | `PUT` | `/api/admins/{id}` | Update Admin profile | Admin |
| **Guests** | `POST` | `/api/guests/register` | Register a new Guest | Public |
| | `GET` | `/api/guests` | List all Guests | Admin |
| | `PUT` | `/api/guests/{id}` | Update Guest profile | Guest/Admin |

---

## 🔧 Maintenance & Self-Healing
If the application encounters database ID mismatches (common in certain legacy MySQL environments), the built-in `DatabaseInitializer` and `StringLongConverter` will automatically sanitize and repair the schema on the fly. 

> **Tip**: If you receive a `403 Forbidden` after a long session, simply re-run the **Login** request to refresh your JWT token.

---
*© 2026 Group F - Advanced User Management Module*

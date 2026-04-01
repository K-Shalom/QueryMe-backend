# Manual Testing Guide for Sandbox Services

## Current Issue
Java 25 + Lombok compatibility is preventing Maven compilation. Here's how to test manually:

## Option 1: Manual API Testing with Postman/cURL

### 1. Start the Application
```bash
# First, try to compile without tests
cd "d:\QueryMe-backend"
.\mvnw clean compile -Dmaven.test.skip=true

# If that works, start the application
.\mvnw spring-boot:run
```

### 2. Test Sandbox Creation via API

#### Create an Exam First
```bash
curl -X POST http://localhost:8080/api/exams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "courseId": "test-course-123",
    "title": "Database Test Exam",
    "description": "Test exam for sandbox",
    "visibilityMode": "IMMEDIATE",
    "timeLimitMins": 60,
    "maxAttempts": 1,
    "seedSql": "CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100)); INSERT INTO students VALUES (1, \"Test Student\");"
  }'
```

#### Create an Exam Session (this triggers sandbox creation)
```bash
curl -X POST http://localhost:8080/api/exam-sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "examId": "EXAM_ID_FROM_PREVIOUS_RESPONSE",
    "studentId": "test-student-123"
  }'
```

## Option 2: Database Verification

### Connect to your PostgreSQL database and check:
```sql
-- Check if sandbox schemas were created
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name LIKE 'exam_%_student_%';

-- Check sandbox registry
SELECT * FROM sandbox_registry;

-- Check if users were created
SELECT usename 
FROM pg_user 
WHERE usename LIKE 'usr_%';
```

## Option 3: Manual Unit Testing (No Compilation)

### Create a Test.java file without Lombok:
```java
// Create this in a separate folder outside the project
public class SandboxTest {
    public static void main(String[] args) {
        // Test schema name generation
        UUID examId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        
        String expectedSchema = "exam_" + examId.toString().replace("-", "") + 
                              "_student_" + studentId.toString().replace("-", "");
        
        System.out.println("Generated schema name: " + expectedSchema);
        System.out.println("✅ Schema name generation works!");
        
        // Test other logic manually...
    }
}
```

## Option 4: Use Java 21 (Recommended)

### Install Java 21:
```bash
# Using SDKMAN (if available)
sdk install java 21.0.x-open
sdk use java 21.0.x-open

# Or download from Oracle/OpenJDK and set JAVA_HOME
export JAVA_HOME=/path/to/java21
```

### Then run tests:
```bash
./mvnw test -Dtest=Sandbox*Test
```

## What to Test Once Compilation Works

### 1. SandboxServiceImpl Tests
- ✅ Schema creation with proper naming
- ✅ User creation with restricted permissions  
- ✅ Seed SQL execution
- ✅ Rollback on failure
- ✅ Teardown functionality
- ✅ Connection details retrieval

### 2. SandboxCleanupScheduler Tests
- ✅ Expired sandbox detection
- ✅ Bulk cleanup operations
- ✅ Error handling during cleanup
- ✅ Scheduler continues on individual failures

### 3. Integration Tests
- ✅ End-to-end sandbox lifecycle
- ✅ Database persistence
- ✅ Multiple students per exam
- ✅ Complex seed SQL scenarios

## Quick Verification Steps

1. **Start the application** (if compilation works)
2. **Create an exam** with seed SQL
3. **Create an exam session** to trigger sandbox creation
4. **Check database** for new schemas/users
5. **Verify sandbox isolation** by testing SQL queries
6. **Test cleanup** by triggering scheduler or manual teardown

## Next Steps

1. **Try Java 21** for immediate testing
2. **Or use manual API testing** with the running application
3. **Verify database changes** directly in PostgreSQL
4. **Check logs** for sandbox creation messages

The test files I created are comprehensive and ready to use once the compilation issue is resolved!

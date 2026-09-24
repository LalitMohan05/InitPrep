# InitPrep

InitPrep is an AI-powered interview preparation platform built using a
microservice architecture. It combines coding practice, automated code
execution, interview attempts, and AI-powered feedback into a single platform.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot | Microservices |
| Spring Cloud Gateway | API Gateway |
| Spring AI | AI integration |
| Gemini | AI-powered feedback |
| React + TypeScript | Frontend |
| PostgreSQL | Database |
| Judge0 | Code execution |
| Maven | Build & dependency management |

---

## Services

| Service | Port | Responsibility |
|---|---:|---|
| Auth Service | `8081` | Authentication, JWT, roles |
| User Service | `8082` | User profiles |
| Interview Service | `8083` | Questions and test cases |
| Attempt Service | `8084` | Submissions, attempts, results, AI feedback |
| Judge Service | `8085` | Code execution through Judge0 |
| AI Service | `8086` | AI-powered coding feedback |
| API Gateway | `8080` | Single entry point for client requests |

---

# API Reference

## Auth Service — `8081`

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate user and generate JWT |
| `GET` | `/api/auth/me` | Get authenticated user |

---

## User Service — `8082`

### User Profile

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/users/profile` | Create user profile |
| `GET` | `/api/users/profile` | Get current user's profile |
| `PATCH` | `/api/users/profile` | Update current user's profile |

---

## Interview Service — `8083`

### Questions

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/questions` | Create question |
| `GET` | `/api/questions` | Get questions with filtering/pagination |
| `PATCH` | `/api/questions/{questionId}` | Update question |
| `DELETE` | `/api/questions/{questionId}` | Delete question |
| `GET` | `/api/questions/{questionId}/exists` | Check whether question exists |
| `GET` | `/api/questions/{questionId}/details` | Get question details |
| `GET` | `/api/questions/{questionId}/judge-data` | Get question data required for judging |

### Test Cases

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/questions/{questionId}/test-cases` | Create test case |
| `GET` | `/api/questions/{questionId}/test-cases` | Get test cases |
| `PATCH` | `/api/questions/{questionId}/test-cases/{testCaseId}` | Update test case |
| `DELETE` | `/api/questions/{questionId}/test-cases/{testCaseId}` | Delete test case |
| `POST` | `/api/questions/{questionId}/test-cases/bulk` | Create multiple test cases |

---

## Attempt Service — `8084`

### Attempts

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/attempts` | Create and evaluate an attempt |
| `POST` | `/api/attempts/run` | Execute code |
| `GET` | `/api/attempts` | Get user's attempts |
| `GET` | `/api/attempts/{attemptId}` | Get a specific attempt |
| `GET` | `/api/attempts/{attemptId}/ai-feedback` | Get AI feedback for an attempt |

### AI Feedback Behavior

AI feedback is generated lazily.

```text
GET /api/attempts/{attemptId}/ai-feedback
                    |
                    v
          Check existing feedback
                    |
             +------+------+
             |             |
           Exists       Not Found
             |             |
             v             v
          Return       AI Service
           from DB         |
                           v
                         Gemini
                           |
                           v
                     Save to DB
                           |
                           v
                         Return
```

## Judge Service — `8085`

Judge Service is responsible for executing submitted code and evaluating it
against test cases.

### Supported Languages

- Java
- Python
- C++
- C
- JavaScript

### API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/judge/submissions` | Execute code against test cases |

### Responsibilities

- Compile submitted code
- Execute test cases
- Detect compilation errors
- Detect runtime errors
- Detect time limit exceeded
- Detect memory limit exceeded
- Compare actual and expected output
- Track passed and failed test cases
- Aggregate execution results

Judge Service uses **Judge0** as the code execution engine.

---

## AI Service — `8086`

AI Service provides coding interview feedback using Spring AI and Gemini.

### API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ai/coding-feedback` | Generate AI-powered coding feedback |

### Input

The AI Service receives:

- Question
- Candidate code
- Programming language
- Judge status
- Passed test cases
- Failed test case
- Compiler output
- Runtime output

### Output

The AI generates:

- Summary
- Mistake
- Explanation
- Suggestion
- Complexity analysis
- Optimized approach

---

## API Gateway — `8080`

The API Gateway provides a single entry point for client requests.

### Routes

| Gateway Route | Target Service |
|---|---|
| `/api/auth/**` | Auth Service `8081` |
| `/api/users/**` | User Service `8082` |
| `/api/questions/**` | Interview Service `8083` |
| `/api/attempts/**` | Attempt Service `8084` |

Judge and AI Services are currently internal services and are accessed directly
by Attempt Service.

---

## Service Communication

Current service-to-service communication uses synchronous HTTP.

```text
Attempt Service
      |
      +----> Interview Service
      |
      +----> Judge Service
      |
      +----> AI Service
```
### Service Communication

Services communicate using:

- **REST APIs**
- **DTOs**
- **UUID identifiers**

Services do not share JPA entities or database relationships.

---

## Database Ownership

Each service owns and manages its own data.

| Service | Data |
|---|---|
| Auth Service | Authentication data |
| User Service | User profile data |
| Interview Service | Questions, companies, topics, test cases |
| Attempt Service | Attempts, judge results, AI feedback |

---

## Core User Flow

```text
Login
  ↓
Browse Questions
  ↓
Select Question
  ↓
Write Solution
  ↓
Submit Attempt
  ↓
Attempt Service
  ↓
Judge Service
  ↓
Judge0
  ↓
Execution Result
  ↓
Request AI Feedback
  ↓
AI Service
  ↓
Gemini
  ↓
AI Feedback
  ↓
Saved in Database

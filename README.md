# be-interview-prep

Backend interview prep assignment: five Spring Boot features, each delivered as its own branch and pull request.

## Tech stack
- Java 17, Spring Boot 3.5
- Maven (wrapper included)
- H2 in-memory database
- JUnit 5, MockMvc, AssertJ

## Prerequisites
- JDK 17+
- No database or Maven install needed (H2 in-memory + `./mvnw`)

## Run the app
```bash
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```
The API listens on `http://localhost:8080`. Health check: `GET /actuator/health`.

## Run the tests
```bash
./mvnw verify                   # Windows: .\mvnw.cmd verify
```

## API overview

All errors share one JSON shape:
```json
{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
 "path": "/api/tasks", "fieldErrors": [{"field": "title", "message": "Title is required"}]}
```

### Q1 — Tasks
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/tasks` | 201; `title` required (≤100), `dueDate` not in the past, `status` defaults to `TODO` |
| GET | `/api/tasks?status=DONE` | list, optional status filter (`TODO`, `IN_PROGRESS`, `DONE`) |
| GET | `/api/tasks/{id}` | 404 if unknown |
| PUT | `/api/tasks/{id}` | full update; omitted `status` keeps the current one |
| DELETE | `/api/tasks/{id}` | 204 |

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#6](https://github.com/rakeshET/be-interview-prep/pull/6) |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**

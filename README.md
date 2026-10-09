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

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**

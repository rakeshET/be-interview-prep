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
The JWT signing secret is never committed; provide it (at least 32 characters) via `JWT_SECRET`.
Optionally seed an admin with `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
```bash
export JWT_SECRET=$(openssl rand -base64 48)
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=change-me-please   # optional
./mvnw spring-boot:run
```
PowerShell: `$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }) -as [byte[]]); .\mvnw.cmd spring-boot:run`
The API listens on `http://localhost:8080`. Health check: `GET /actuator/health`.

## Run the tests
```bash
./mvnw verify                   # Windows: .\mvnw.cmd verify
```
Tests generate their own random JWT secret, so no environment variables are needed.

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

### Q2 — URL shortener
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/urls` | 201; body `{"url": "https://...", "expiresAt": "2030-01-01T00:00:00Z"}` (expiry optional) → `code`, `shortUrl` |
| GET | `/r/{code}` | 302 redirect to the original URL and counts the visit; 404 unknown, 410 expired |
| GET | `/api/urls/{code}/stats` | original URL, visit count, created date |

### Q3 — Authentication & roles
All `/api/**` endpoints (Q1, Q2, ...) require `Authorization: Bearer <token>`; the `/r/{code}` redirect stays public.
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/auth/register` | 201; `{"email", "password"}` (8–72 chars); always creates a `USER`; 409 if the email exists |
| POST | `/api/auth/login` | `{"accessToken", "tokenType": "Bearer", "expiresIn": 900}` (15 minutes) |
| GET | `/api/users/me` | the caller's profile (any role) |
| GET | `/api/admin/users` | all users, `ADMIN` only (403 for `USER`) |

### Q4 — Product catalog
100 products are seeded on startup. Reads need any logged-in user; create/update/delete need `ADMIN`.
| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/products?category=&minPrice=&maxPrice=&inStock=true&q=&page=0&size=20&sort=price,desc` | all filters optional and combinable; `size` capped at 100; sort by `id,name,category,price,stock,rating,createdAt`; response has `totalElements` and `totalPages` |
| GET | `/api/products/{id}` | cached (Caffeine); evicted after an update/delete commits |
| POST / PUT / DELETE | `/api/products[/{id}]` | `ADMIN` only |

To see the cache working, run with `--logging.level.org.hibernate.SQL=debug` and call `GET /api/products/1` twice: only the first call logs a `select`.

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#6](https://github.com/rakeshET/be-interview-prep/pull/6) |
| 2 | URL Shortener | [#7](https://github.com/rakeshET/be-interview-prep/pull/7) |
| 3 | Authentication & Roles | [#8](https://github.com/rakeshET/be-interview-prep/pull/8) |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**

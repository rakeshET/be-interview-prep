# be-interview-prep

Backend interview assignment: 5 Spring Boot features (Q1–Q5), each shipped as its own branch + PR.
GitHub: `rakeshET/be-interview-prep` (public). The assignment spec is in `docs/ASSIGNMENT.md` — it is the source of truth for requirements and acceptance criteria.

## Stack
- Java 17, Spring Boot 3.5.x, Maven wrapper (`./mvnw`; PowerShell: `.\mvnw.cmd`), H2 in-memory DB
- Build + all tests: `./mvnw -q verify`
- Single test class: `./mvnw -q test -Dtest=ClassName`
- Run app: `./mvnw spring-boot:run` (needs env `JWT_SECRET` once Q3 is merged)
- Base package: `com.edstem.interviewprep`

## Code conventions
- Package by layer: `config/` (Spring config, `@ConfigurationProperties`, seeders), `controller/`, `service/`, `repository/` (incl. JPA specifications), `entity/` (JPA entities + enums), `dto/request/`, `dto/response/`, `exception/` (domain exceptions, `ApiError`, `GlobalExceptionHandler`), `security/` (JWT token service, security error handlers), `validation/` (custom constraints). Tests mirror the package of the class under test.
- Layering: `controller → service → repository`. Thin controllers; business logic in services.
- Request/response DTOs as Java `record`s; never expose JPA entities from controllers.
- Bean Validation on all input (`@Valid`, `@NotBlank`, `@Size`, ...).
- All errors go through `exception/GlobalExceptionHandler` and return the shared `ApiError` JSON with the right HTTP status. Throw domain exceptions (e.g. `NotFoundException`), don't build error responses in controllers.
- Constructor injection only. No secrets in code or `application.yml` — read them from env vars.
- No comments in Java code. Use clear names instead, and put the reasoning behind design decisions in the PR's "Decisions & trade-offs".

## Tests (mandatory in every PR)
- Every PR must add tests for the behaviour it introduces, covering the acceptance criteria and the error paths.
- JUnit 5 + AssertJ + Mockito. `@WebMvcTest`/MockMvc for HTTP contracts, `@SpringBootTest` for flows and concurrency, plain unit tests for services.
- `./mvnw verify` must be green before a PR is raised. Never disable or skip tests.

## Git conventions (from the assignment)
- Branch per question, created from the latest `main`, with these exact names:
  `feature/q1-task-api`, `feature/q2-url-shortener`, `feature/q3-auth`, `feature/q4-product-catalog`, `feature/q5-order-service`.
- Small, meaningful commits with plain imperative messages: `Add create task endpoint`, `Return field errors for invalid input`. Never `fix`, `changes`, `final`, `wip`.
- PR description uses the template in `.github/pull_request_template.md` (Problem / Approach / Decisions & trade-offs / How to test) and contains `Closes #<issue>`.
- **Claude never merges PRs — the user merges.** Do not start the next question until the previous PR is merged and `main` is pulled.
- Never force-push; never commit to `main` after the base setup.
- Keep `README.md` updated: each PR fills in its own row in the PR-link table.

## Workflow (skills)
1. `/create-issue Q<n>` (or free text) — GitHub issue built from the assignment's requirements + acceptance criteria.
2. `/start-issue <issue#>` — branch `feature/q<n>-...` from latest `main`, plan, implement with tests, green build, commits.
3. `/raise-pr [issue#]` — verify, update README row, push, open the PR. Stop: the user merges.
4. `/pr-review [pr#]` — review against the question's acceptance criteria and the interview questions; post as a GitHub review.

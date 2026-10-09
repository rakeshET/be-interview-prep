# be-interview-prep

Backend interview test project. GitHub: `rakeshET/be-interview-prep`.

## Stack
- Java 21, Spring Boot 3, Maven (use the wrapper: `./mvnw`; on PowerShell use `.\mvnw.cmd`)
- Build + all tests: `./mvnw -q verify`
- Single test class: `./mvnw -q test -Dtest=ClassName`
- Run app: `./mvnw spring-boot:run`

If the Spring Boot skeleton does not exist yet, generate it with Spring Initializr
(`curl https://start.spring.io/starter.zip -d type=maven-project -d javaVersion=21 -d dependencies=web,validation,data-jpa,h2,actuator -o starter.zip`) and keep the Maven wrapper.

## Code conventions
- Layering: `controller → service → repository`. Controllers stay thin; business logic lives in services.
- Package by feature under the base package (e.g. `.../order/OrderController.java`, `OrderService`, `OrderRepository`, `dto/`).
- Request/response DTOs at the API boundary (Java `record`s); never expose JPA entities from controllers.
- Validate input with Bean Validation (`@Valid`, `@NotNull`, `@Size`, ...).
- Centralised error handling in one `@RestControllerAdvice`, returning `ProblemDetail`; correct HTTP status codes.
- Constructor injection only (no field `@Autowired`).
- No secrets in code or `application.yml`; use env vars.
- Tests: JUnit 5 + AssertJ + Mockito. Unit-test services; `@WebMvcTest` for controllers; `@DataJpaTest` for custom queries. Every new behaviour needs a test, including error paths.

## Git conventions
- Never commit directly to `main` (the one-time harness bootstrap commit is the only exception).
- One issue per branch, one branch per PR.
- Branch name: `<type>/<issue#>-<short-slug>`, type ∈ `feat|fix|chore|refactor|test|docs` (e.g. `feat/12-create-order-endpoint`).
- Conventional Commits referencing the issue: `feat(order): add create endpoint (#12)`.
- PR body must contain `Closes #<issue>`.
- Never force-push.

## Workflow (skills)
1. `/create-issue <description>` — write a well-specified GitHub issue with acceptance criteria.
2. `/start-issue <issue#>` — branch off latest `main`, plan, implement with tests, build green, commit.
3. `/raise-pr [issue#]` — verify build, push, open (or update) the PR linked to the issue.
4. `/pr-review [pr#]` — review the PR against the issue and these conventions, post findings to GitHub.

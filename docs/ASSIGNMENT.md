# Assignment spec (transcribed from Backend_Interview_Prep_Assignment.pdf)

Stack: Java 17+, Spring Boot 3.x. Time limit: 2 hours for all 5 questions incl. PRs and merges.
Assessed: working code, clean structure, tests, Git/PR discipline, and understanding of what was built.

Interviewers will ask, per PR:
- Walk me through what happens from the HTTP request to the database.
- Why did you choose this approach? What alternatives did you consider?
- What breaks if two requests hit this endpoint at the same time?
- What happens if you remove this line?
- Change this behaviour live.

## Q1 — Task Manager API  (branch `feature/q1-task-api`)
Build a REST API to create, view, update, delete and filter tasks.

Requirements
- A task has a title (required, max 100 characters), a description, a status (To do / In progress / Done), a due date (cannot be in the past) and a created date.
- Create, list, get one, update and delete tasks. Filter the list by status.
- Reject invalid input with a clear message for each invalid field.
- All errors (invalid input, not found, unexpected) return one consistent JSON format with the right HTTP status.

Acceptance criteria
- Invalid input returns 400 with field-level messages. An unknown task returns 404.
- At least one automated test.

## Q2 — URL Shortener  (branch `feature/q2-url-shortener`)
Build a service that turns long URLs into short links.

Requirements
- Submit a long URL, optionally with an expiry date, and get back a short code and a short URL.
- Visiting the short URL redirects to the original URL.
- Count every visit. A stats endpoint shows the original URL, the visit count and the created date.
- Short codes are at most 8 characters, unique and URL-safe.
- Reject invalid URLs. Handle unknown and expired codes with an appropriate status.

Acceptance criteria
- Shortening the same URL twice behaves the way you decided it should, and you can explain why.
- Visit counts stay accurate when many people open the same link at once.
- At least one automated test.

## Q3 — Authentication & Roles  (branch `feature/q3-auth`)
Secure an API so that only logged-in users can use it, and some endpoints are admin-only.

Requirements
- Users can register and log in. Store passwords securely.
- The API is used by web and mobile clients, so authentication must not rely on server-side sessions.
- Login expires after 15 minutes.
- Two roles, USER and ADMIN. Any logged-in user can view their own profile. Only an ADMIN can list all users.
- A request that isn't logged in returns 401. A logged-in user without the right role gets 403. Both return JSON, not an HTML error page.

Acceptance criteria
- A test proves that a USER cannot access the admin endpoint.
- No secrets are hard-coded in the source.

## Q4 — Product Catalog  (branch `feature/q4-product-catalog`)
Build a product listing API that stays fast as the catalog grows.

Requirements
- A product has a name, category, price, stock, rating and created date. Seed 100 products on startup.
- List products with pagination and sorting by any field. The response includes the total count and the number of pages.
- Optional filters that can be combined freely: category, price range, in-stock only, and name search.
- The page size is capped at 100.
- Single-product lookups happen far more often than products change. Make repeated lookups fast, but never return stale data after a product is updated or deleted.

Acceptance criteria
- Any combination of filters works in a single request.
- Repeated lookups of the same product don't query the database every time. Be able to show how you know.
- At least one automated test.

## Q5 — Order Service  (branch `feature/q5-order-service`)
Build an order API that stays correct under heavy, simultaneous use.

Requirements
- Products have limited stock. A customer places an order with one or more items.
- An order is all-or-nothing: either every item is reserved, or none is.
- Stock must never go negative or be oversold, even when many customers order the same product at the same moment.
- Clients may retry a request after a network failure. A retried request must not create a duplicate order. Design how a retry is recognised.
- Insufficient stock returns 409 with a clear message.
- Cancelling an order returns its stock.

Acceptance criteria
- An automated test fires 50 simultaneous orders for a product with stock 10. Exactly 10 succeed, and the stock ends at 0.
- Retrying the same request creates only one order.

## Optional (only after all 5)
- Q1: interactive API documentation. Q2: custom short codes. Q3: refresh tokens + logout.
- Q4: caching across multiple instances. Q5: second concurrency approach + comparison; tests on a real DB.

## Submission checklist
- Public repo with README; 5 branches, 5 PRs, all merged into main; every PR uses the template.
- README explains how to run the app and the tests, and has every PR link and the YouTube video link.
- All tests pass.

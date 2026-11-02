# Simplification verification — 2 November 2026

This entry applies to the simplification branch based on main at 0017925. The dated 30 October record below describes the earlier implementation.

| Check | Result |
| --- | --- |
| Maven clean verify, then verify after final Java changes, TEST_DATABASES=true | PASS: all five services built; 28 tests, 0 failures/errors/skips |
| Student / Course / Enrollment / HTTP client tests | 6 / 4 / 12 / 6 passed against PostgreSQL 17.9 where persistence is involved |
| New race cases | PASS: simultaneous first-row creation, simultaneous duplicate creation, student and course closure before enrollment takes its lock |
| Persisted profile/course updates | PASS: strengthened existing CRUD tests modify values and retrieve them in a subsequent HTTP request; both suites rerun successfully |
| Cancellation / security / dependency failures | PASS: original boundary, JWT, ownership, direct-service and missing/unavailable dependency assertions retained |
| Angular tests | PASS: 2 files, 4 tests |
| Angular production build | PASS |
| Applied Flyway SQL | Unchanged byte-for-byte in Git; validation passed on existing test schemas |
| Application and test Compose configuration | PASS |
| Fresh Docker Compose stack | PASS: all six application images built; five Java processes, Nginx and three separate PostgreSQL containers started with fresh volumes |
| Fresh schema migrations | PASS: native Flyway/JPA startup in the fresh containers; existing reference-table mapping requires no new migration |
| Java 17 runtime | PASS: application ran in the supplied eclipse-temurin:17-jre containers; local Maven used Java 21 targeting Java 17 |
| Live scripts/smoke.mjs against Compose | PASS: Eureka registrations, Gateway CRUD/login/provisioning, direct-service 401s, role/ownership restrictions, enrollment/duplicates, deletion 409s, cancellation/double cancellation and eligible deletions |
| Frontend integration | PASS: production HTML served by Nginx; authenticated /api traffic proxied to Gateway |
| Working tree / migration / whitespace review | PASS; runtime secrets, build products and test databases excluded from the commit |

Compose verification used project enrollment-simplification, fresh project-owned database volumes and random credentials stored only under ignored .local. A temporary ignored override exposed domain ports 18081–18083 to the smoke script; committed Compose still keeps domain ports private. The smoke check was run after discovery became ready; initial registry/cache warm-up is still expected. Test containers/volumes and the isolated native PostgreSQL test process were stopped afterward. The existing user PostgreSQL service and application databases were not touched.

No tests were weakened or deleted. Removed stubs only covered the redundant second student GET; renamed mock calls follow the more explicit deletion-client names. New assertions verify actual dirty-checking persistence, and four additional parameterized cases exercise PostgreSQL concurrency.

Browser visual/interaction checks and the remote GitHub Actions execution were not run for this refactoring. The HTTP smoke test validates the production frontend proxy, not rendered form interaction. Original JWT revocation/shared-secret, legacy timezone and cross-service partial-deletion limitations remain. See simplification-guide.md and README.

---

# Verification record — 30 October 2026

## Executed successfully

| Check | Actual result |
| --- | --- |
| Maven clean build and tests, followed by verify on the final backend sources | All five services compiled/packaged; 24 tests passed, 0 failures/errors/skips with TEST_DATABASES=true |
| StudentApiTest | 6 tests: CRUD/missing resources, validation/duplicates, guarded deletion/dependency failure, direct permissions, real login/provisioning, signed-token issuer/audience/expiry/signature rejection |
| CourseApiTest | 4 tests: CRUD/missing resources, validation/duplicates, guarded deletion/dependency failure, direct JWT/role protection |
| EnrollmentApiTest | 8 tests: creation/duplicate/capacity, missing/dependency failures, 4 overlapping requests for the last seat, cancellation before/at/after deadline and double cancellation, ownership, deletion guards, enrollment-versus-deletion overlap, database duplicate constraint |
| ClientTest | 6 tests: success, 404, unauthorized dependency, 500 dependency, invalid body, timeout |
| Angular npm ci | Installed 474 packages from the existing lockfile |
| Angular npm test -- --watch=false | 2 files, 4 tests passed |
| Angular npm run build | Production build passed; initial bundle approximately 375 kB, within configured budgets |
| Flyway clean database startup | Student, Course and Enrollment schemas migrated and Hibernate validation passed against PostgreSQL 17.9 |
| Legacy migration SQL | Each migration ran against a separate synthetic original schema inside a rolled-back transaction; existing profiles/courses/enrollments were preserved, including the documented UTC timestamp interpretation |
| Native application startup | Eureka, all three domains, Gateway and Angular started successfully |
| Live Eureka registry | STUDENT-SERVICE, COURSE-SERVICE, ENROLLMENT-SERVICE and API-GATEWAY registered UP |
| Discovery without default ports | Domains ran on **18081, 18082 and 18083**. Gateway and internal clients kept their default service-name configuration; requests succeeded |
| scripts/smoke.mjs | Passed Gateway CRUD, real administrator/student login, account provisioning, profile ownership, admin-only permissions, direct-service 401 checks, enrollment/duplicate/cancellation, active-reference 409 checks and eligible deletions |
| Angular HTTP integration | UI HTML served successfully; /api calls through Angular's development proxy reached Gateway and returned authenticated data |
| Live unavailable Course Service | Stopped only the isolated demo Course process; enrollment returned 503 in approximately 2.1 seconds and did not persist a record |
| Live unavailable Enrollment Service | Stopped only the isolated demo Enrollment process; student deletion returned 503 and the profile remained intact |
| Compose configuration | Both application and test Compose files passed docker compose config validation |
| Git whitespace review | Passed after removing trailing whitespace |

Local tools: Maven 3.9.11, Java 21.0.11 compiling with release 17, Node 20.19.6, PostgreSQL 17.9. GitHub Actions is configured separately for Java 17 and Node 22.

Testing used an isolated PostgreSQL cluster bound to 127.0.0.1:55432, with separate *_test and *_demo databases. The user's existing PostgreSQL service and application databases were not changed. Random runtime credentials and logs were kept under ignored .local and excluded from Git.

## Checks not verified

- **Browser interaction/visual QA:** the in-app browser reached the /login URL but repeated browser-control requests timed out. Native Chrome activation also failed. Login, form submissions and cancellation were verified through HTTP and Angular unit tests, not a successful automated browser session. No claim is made that all rendered screens were visually inspected.
- **Docker runtime:** the Docker client is installed but its Linux daemon was unavailable. Images were not built or run. Native execution verified the application; Compose syntax validation is not a container startup test.
- **GitHub Actions execution:** the workflow was authored but its remote run was not observed locally.
- **Java 17 runtime:** local execution used Java 21 with Java 17 bytecode targeting. The Java 17 CI/runtime-image configuration was not executed locally.
- **Real historical data upgrade:** synthetic legacy data was checked. The actual existing databases were intentionally left untouched. Their timestamp timezone, duplicate records and old orphan references require the upgrade review documented in README.
- **Dependency vulnerability scan:** no exhaustive CVE scan was performed. Framework baseline versions were retained; this is not a claim of production readiness.

## Remaining design limits

JWTs remain valid until expiry and clock tolerance; shared HMAC keys trust every domain service. There is no public registration, refresh token, password recovery, role management interface, rate limiter or external identity service.

Deletion is coordinated through a permanent Enrollment-owned reference closure. A crash/timeout after closure can leave an existing profile/course blocked for new enrollments. Administrator DELETE retries complete the operation safely. Independent database restoration or manual data changes require reconciliation.

The maximum remains the original fixed three seats. Every persisted enrollment counts as active until cancelled; no historical status/archive model was invented.

## Repeat live checks

Start the stack and wait for Eureka registrations/cache refresh. In PowerShell:

```powershell
$env:SMOKE_ADMIN_USERNAME = '<your demo administrator>'
$env:SMOKE_ADMIN_PASSWORD = '<your demo password>'
node scripts/smoke.mjs
```

Use a disposable environment: the script creates and removes fixture profiles/courses/enrollments. Optional SMOKE_API_URL, SMOKE_FRONTEND_URL, SMOKE_EUREKA_URL and SMOKE_STUDENT_PORT / SMOKE_COURSE_PORT / SMOKE_ENROLLMENT_PORT support other addresses and non-default domain ports. The frontend HTTP check expects either the Angular dev server or the supplied Nginx proxy.

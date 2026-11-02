# Original audit and implemented changes

For the subsequent JPA simplification, see [simplification-guide.md](simplification-guide.md). This audit records the original implementation problems.

The audit covered all five Maven modules, their Java sources/configuration, Angular components/services/models/routing, existing tests and repository documentation. The initial branch was main with one preexisting modification: Gateway's routes had already been changed to lb:// service names. That change was retained on codex/enrollment-reliability. AGENTS.md and task files were excluded as requested.

## Verified original problems

| Original location | Finding | Implemented change |
| --- | --- | --- |
| Domain pom.xml files and all controllers | No Spring Security, account model, login or ownership checks; direct domain APIs were public | Spring Security JWT resource servers in each domain, ADMIN/STUDENT permissions, separate accounts in Student Service, Angular login |
| enrollment-service/config/WebClientConfig and application.properties | Plain WebClients used fixed localhost ports while Student Service's EnrollmentClient used a discovery-aware builder | Load-balanced synchronous RestClient throughout, explicit LoadBalancer dependencies, configurable direct-test mode |
| enrollment-service/client/StudentClient and CourseClient | Every downstream 4xx was reported as a missing resource; calls blocked without configured timeouts | Only 404 means missing; unavailable/unauthorized/malformed dependency responses become 503; 2s connect and 5s read timeouts |
| EnrollmentService.enrollStudent | Count/check/insert had no transaction or database lock | Transactional stable student/course reference rows; per-course row lock protects capacity; unique database index protects duplicates |
| EnrollmentService.cancelEnrollment | LocalDateTime.now, no injected clock, no row lock | UTC Instant/Clock; exact original strict 24-hour deadline retained; locked cancellation |
| StudentService.deleteStudent and EnrollmentController | Remote cascade deletion could remove all enrollments, bypassing the cancellation window | Remove cascade endpoint; reject deletion with active references |
| CourseService.deleteCourse | Deleted courses without checking enrollments | Same guarded deletion policy as students |
| StudentController and StudentService | No update operation | Validated administrator PUT endpoint; immutable CNIE; existing forms now support editing |
| CourseService.updateCourse | Renaming to an existing title bypassed the create-only title check; no database uniqueness guarantee | Update check and unique title index |
| Domain GlobalExceptionHandler classes | Missing resources, duplicates and full courses all became 400 | Consistent 400/404/409/503 responses and generic unexpected failures |
| Domain application.properties and README | Database password committed, Hibernate schema mutation on startup | Environment credentials, Flyway migrations, Hibernate validate |
| Three CorsConfig files | Empty configuration classes added no behavior | Removed; Gateway handles CORS with one configurable origin |
| Student test | Only contextLoads, dependent on a developer's live database | PostgreSQL API/JWT/CRUD tests plus enrollment concurrency/failure tests |
| Angular app.spec.ts | Imported nonexistent App class and asserted a removed starter title | Current shell test and authentication/interceptor tests |
| Angular environment.ts | Fixed localhost Gateway URL | Relative /api URL and development/Nginx proxy |
| Navbar and sidebar | Fake static administrator identity and unused controls/imports | Actual session role, sign-out and role-aware navigation |
| README and explanation.md | Claimed Angular 18 (actual dependencies are 21.2), incorrect error/deletion details | Architecture, migration, security, startup, limitations and interview guide rewritten |
| Repository build/setup | No aggregator, migrations, Compose or CI | Maven aggregator, versioned migrations, optional Compose and GitHub Actions |

The code did not contain service interfaces with one implementation, factories or a custom architecture framework. Useful DTOs and small explicit mappers were retained. No arbitrary pattern removal was necessary. WebFlux was removed from the synchronous domain services; Gateway retains its required reactive stack.

## Reliability reasoning and limits

Capacity and duplicate bugs were established by code inspection and then verified against PostgreSQL using concurrent requests and a direct duplicate insertion. Deletion now has database-backed coordination, not merely a remote count followed by a delete. The close-then-delete sequence is deliberately fail-closed and has a retry requirement after partial failure; it is not distributed ACID.

The exact timezone of preexisting LocalDateTime values cannot be determined from the original schema. Migration instructions require an explicit timezone decision before upgrading old data. Existing live data was not modified during validation; isolated databases were used.

No claim is made that the retained framework/dependency versions are free of published vulnerabilities. This refactoring preserves the project's compatible framework baseline; dependency lifecycle/security patch review remains separate maintenance work. The implemented controls are appropriate for a local academic demonstration, with the shared-secret and token-revocation limitations documented in README.

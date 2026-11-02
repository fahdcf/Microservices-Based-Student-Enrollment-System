# Simplification guide

This refactoring starts from main at `0017925`. The architecture stays Angular → Gateway → Student/Course/Enrollment → separate PostgreSQL databases, with Eureka discovery. Within each domain, the flow remains Controller → Service → Repository. No service, API endpoint, permission or business rule was removed.

## What changed

| Files | Before | After |
| --- | --- | --- |
| Enrollment `ReferenceGuard` | Custom JDBC component: string resource types, manual SELECT/UPDATE and transaction propagation annotation | Removed; `EnrollmentReferenceRepository` uses Spring Data JPA and `@Lock(PESSIMISTIC_WRITE)` |
| `EnrollmentReference`, `EnrollmentReferenceId` | Reference rows existed only as SQL | Ordinary JPA entity with the existing composite primary key mapped using `@IdClass` |
| `EnrollmentService.enrollStudent` | Generic lock calls and a second student HTTP GET | Explicit `lockStudent`/`lockCourse`, visible closed-state checks, one student lookup |
| `EnrollmentService.closeStudent/closeCourse` | Guard performed an SQL UPDATE before checking active enrollments | Service locks the entity, checks enrollments, then sets `closed`; JPA saves the change on commit |
| `EnrollmentService` ownership checks | Two copies of role/identity checks | One private method used by enrollment and cancellation; method security retained |
| `EnrollmentRepository` | Fully qualified annotation names | Familiar imported `@Lock`, `@Query` and `Optional` |
| `StudentClient` | Two almost identical GET methods | Removed unused `getStudentById`; retained clear 404/503 handling |
| Student/Course `EnrollmentClient` | Ambiguous `closeReference` | `closeStudentForEnrollment` / `closeCourseForEnrollment`; same HTTP endpoints |
| `StudentService.updateStudent`, `CourseService.updateCourse` | Save a loaded entity again | Transactional updates with JPA dirty checking |
| `AuthenticationService` | Repeated token lifetime literal and an unexplained hash inline | Named lifetime/hash constants; BCrypt timing protection and JWT contract retained |
| Three `SecurityConfig` files | Nested conditional expression for required claims | Ordinary if statement; signature, issuer, audience and expiry checks unchanged |
| Angular students/courses/enroll/dashboard | Four identical error-parsing methods | One plain `apiErrorMessage` function; no injected wrapper service or new framework |
| Sidebar, course form, Gateway Maven configuration | Unused logout/router code, callback parameter, Lombok dependency | Removed |

Controllers, DTOs, mappers, exception handlers, RestClient timeout/token configuration, Compose and CI were reviewed. They already perform useful, small jobs. Combining independently deployed services into a shared framework would add coupling, so their small security/client configurations remain local. The two RestClient builder options preserve discovery by default and explicit direct URLs for tests.

## The important before/after

Previously the enrollment method delegated hidden SQL/state checks through arbitrary strings:

```java
references.lockOpen("student", student.getId());
references.lockOpen("course", request.getCourseId());
studentClient.getStudentById(student.getId());
```

Now its decisions are visible in the service:

```java
if (references.lockStudent(student.getId()).isClosed()) {
    throw new ResponseStatusException(CONFLICT, "This student is being deleted or has been deleted");
}
if (references.lockCourse(request.getCourseId()).isClosed()) {
    throw new ResponseStatusException(CONFLICT, "This course is being deleted or has been deleted");
}
```

Deletion is similarly direct:

```java
var student = references.lockStudent(id);
if (enrollmentRepository.existsByStudentId(id)) {
    throw new ResponseStatusException(CONFLICT, "Student has active enrollments");
}
student.setClosed(true);
```

These methods run inside `@Transactional`. The locked reference is a managed entity; Hibernate persists its changed flag when the transaction commits. The repository contains the familiar `@Lock(PESSIMISTIC_WRITE)` queries. Its two short default methods create a missing row, then load it with a lock. The service owns the transaction spanning both steps and the business checks.

## Enrollment, step by step

1. Method security permits an administrator or the token's own CNIE. Student Service resolves the profile and independently checks identity. Enrollment also verifies the student's immutable ID against the token.
2. Lock the student reference, then the course reference, always in that order. Reject either closed reference.
3. Fetch the course by REST. Only downstream 404 means missing; failed, invalid or unauthorized downstream responses remain 503.
4. Reject duplicates and courses with three enrollments. Insert the enrollment in the same transaction.
5. Commit, releasing locks. Another instance waiting on that course now sees the committed count.

The second student GET was redundant: if deletion completed between the first GET and locking, its permanent closed flag rejects enrollment. If enrollment acquired the lock first, deletion must wait. No deletion using the supported APIs can remove the profile without first closing the same reference.

Cancellation still locks the enrollment itself, verifies ownership and compares UTC `Clock.instant()` with enrollment time + 24 hours. Exactly 24 hours is rejected. Deleting the enrollment frees its seat; a second cancellation returns 404.

## Complexity that is necessary

- **Stable reference rows:** Course records are in Course Service's database. Enrollment cannot JPA-lock those remote records or assume its own `@Transactional` includes another service. An Enrollment-owned row gives all Enrollment instances the same lock and remembers a deletion even after the remote record is gone.
- **One narrow PostgreSQL operation, expressed twice for student/course:** `INSERT ... ON CONFLICT DO NOTHING` safely creates the first reference row. A JPA find-then-save could race on the primary key, and catching a uniqueness exception inside that transaction would not repair the aborted PostgreSQL transaction. Locks and updates otherwise use JPA.
- **Composite key:** The existing table identifies a row by type + ID. A student and a course can both have ID 1. The small `EnrollmentReferenceId` class maps that existing primary key without changing applied migrations.
- **Lock wait limit:** Enrollment's Hikari `connection-init-sql` sets PostgreSQL `lock_timeout` to five seconds per connection. This replaces SQL executed on every guard call and bounds waits for both insertion and row locks. No direct JDBC component remains in application code.
- **Database constraints:** Existing primary keys, unique student/course enrollment index, title/email/CNIE uniqueness and reference type constraints remain unchanged. They remain enforced for direct SQL too. Capacity is enforced by the supported transactional service path; arbitrary SQL inserts can bypass capacity, as before.
- **JWT validation in every domain:** Gateway routing and UI buttons alone cannot enforce permissions. Each service still needs a resource-server configuration and ownership checks.

Removing these mechanisms would shorten the code while reintroducing races or permission bypasses. No new shared framework, coordinator service, message broker or locking technology was introduced.

## Deletion and failure recovery

Both owner services first close their reference through Enrollment Service. The reference lock serializes closure against creation. Existing enrollments cause 409; an empty reference becomes permanently closed before the local deletion proceeds. Repeated closure is safe.

This remains a fail-closed two-service operation, not a distributed ACID transaction. A crash or timeout after closure may leave an existing profile/course blocked for enrollment. An administrator retries DELETE. Do not automatically reopen the row: another deletion attempt may still be running. Manual database changes, reused IDs and independent database restores require reconciliation.

Applied Flyway files were not edited and no schema migration was necessary. Existing open and closed reference rows retain their meaning. Existing token lifetime/revocation limits, shared HMAC trust boundary and UTC interpretation of legacy timestamps remain documented in README.

## Verification

See the 2 November entry in `verification.md` for executed results. The original assertions remain; only the stub for the removed second GET and renamed client methods changed. Additional PostgreSQL tests cover simultaneous creation of missing lock rows, concurrent duplicate requests and deletion winning before enrollment acquires a lock, for both student and course.

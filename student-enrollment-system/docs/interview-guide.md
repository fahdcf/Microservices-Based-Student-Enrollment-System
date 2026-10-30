# Explaining the implementation

- **What is a microservice?** A separately running application responsible for one business area. Student, Course and Enrollment each have their own Spring Boot main class and can be deployed separately.
- **Why three domains?** Profiles, the catalogue and registration have distinct rules. Keeping the original three domains demonstrates HTTP communication and independent data ownership without adding services.
- **Why separate databases?** StudentRepository, CourseRepository and EnrollmentRepository own different tables/connections. A change in one domain does not grant other domains direct access to its schema. Only the local user_accounts → students relationship has a foreign key.
- **What does Gateway do?** api-gateway/application.yml maps external paths to lb:// service names. Angular uses /api and does not know domain ports.
- **What does Eureka do?** EurekaServerApplication enables a registry. Clients publish their addresses; Gateway and RestClientConfig use LoadBalancer to choose a registered instance.
- **How do services communicate?** StudentClient and CourseClient make synchronous HTTP GET requests, with the caller's bearer token and bounded connect/read timeouts. A 404 means missing data; network/server/invalid-response errors become 503.
- **How is information saved?** Controllers validate DTOs; service classes apply rules; Spring Data JPA repositories persist entities. Flyway creates the schema; Hibernate validates the entity mapping.
- **Why is capacity safe?** EnrollmentService.enrollStudent locks student then course reference rows through ReferenceGuard.lockOpen. The course lock stays held while counting and inserting, so another transaction must wait. PostgreSQL coordinates locks across application instances.
- **Why keep the unique index?** Business checks give useful messages, but the database independently guarantees at most one enrollment per student/course.
- **How does cancellation work?** cancelEnrollment checks ownership and compares Clock.instant against the persisted UTC timestamp plus 24 hours, while holding a row lock. The exact deadline is excluded.
- **How does deletion work?** StudentService.deleteStudent and CourseService.deleteCourse call EnrollmentClient.closeReference first. EnrollmentService.closeStudent/closeCourse refuses active references and permanently blocks future enrollment. A failure between databases requires an administrator to retry deletion; it is not a distributed transaction.
- **How does login work?** AuthenticationService.login checks a BCrypt hash and issues a 15-minute JWT. SecurityConfig in every domain validates signature/issuer/audience/expiry. Method permissions enforce ADMIN/STUDENT roles; cancellation compares the persisted student ID.
- **Why separate profiles and login accounts?** A profile is academic data. UserAccount is an authentication identity linked to that profile by ID. A CNIE alone is not a password.
- **What happens during an outage?** Dependency calls fail within configured timeouts and return 503. Enrollment and deletion fail closed; a dependency outage is never treated as a missing student/course.
- **How is this different from a monolith?** These are separate processes/databases using discovery and HTTP. There is no cross-service ACID transaction. Each local transaction remains simple Spring/JPA code.

Useful starting points: EnrollmentService, ReferenceGuard, AuthenticationService, each SecurityConfig, StudentClient/CourseClient, the service controllers, and the three *ApiTest classes. Follow one request from its controller to its service, repository and migration.

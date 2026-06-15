# Developer's Guide: Student Enrollment Microservices

This document is the definitive breakdown of how this project was designed, structured, and built from a developer's perspective. It contains deep dives into the source code, architecture decisions, and business rules, answering exactly *how and why* things work under the hood.

---

## 1. System Architecture & Context

The project follows a **Database-Per-Service** microservices architecture. 
Instead of building a monolith, we isolated domains into 3 main actors: Students, Courses, and Enrollments. 
We placed a Gateway layer in front to route external traffic to the correct microservice, and leveraged Eureka to map where those services live on the network without hardcoding IPs.

### The Big Picture:
1. **Frontend App (`4200`)** â†’ User Interface
2. **API Gateway (`8080`)** â†’ Reverse Proxy / Edge Router
3. **Eureka Server (`8761`)** â†’ Service Registry

**Backend Services:**
- `student-service` (Port: `8081`, DB: `student_db`)
- `course-service` (Port: `8082`, DB: `course_db`)
- `enrollment-service` (Port: `8083`, DB: `enrollment_db`)

---

## 2. Infrastructure Layer

### The Service Registry (Eureka)
The Eureka server (`@EnableEurekaServer`) allows our microservices to discover one another automatically. Because of this, our Spring Boot applications don't need to know explicit IP addresses. When `student-service` boots, it pings the Eureka server and says, *"Hi, I am student-service, and I currently live at localhost:8081"*. 

### API Gateway (Spring Cloud Gateway)
The entry point from the Angular application. Based on the URL path, it filters and sends traffic safely to the correct backend service.

*Excerpt from `api-gateway/src/main/resources/application.yml`:*
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: student-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/students/**
        - id: course-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/courses/**
        - id: enrollment-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/enrollments/**
```
**Why this matters:** The Angular frontend only ever sends API fetches to `http://localhost:8080`. It never has to worry about port 8081 or 8083. The Gateway handles the CORS rules globally to allow frontend traffic.

---

## 3. Data & Entity Layer

In microservices, you don't use Foreign Keys constraint dependencies across different services. 
*   In `student-service`, we have a pure `@Entity Student`.
*   In `course-service`, we have a pure `@Entity Course`.
*   In `enrollment-service`, we have an `@Entity Enrollment` that saves the *IDs* of the student and course, but it has no direct database relation to them.

**Student Validation:**
We enforced strict data compliance in the `student-service` directly on the DTO constraints:
```java
// StudentRequest.java (DTO)
@NotBlank(message = "CNIE is required")
@Size(min = 4, max = 8, message = "CNIE must be exactly between 4 and 8 characters")
private String cnie;
```

---

## 4. The Brains: Enrollment Service & Cross-Communication

This is the most complex point in the app. Since `enrollment_db` has no Foreign Keys pointing to `student_db`, how does it know the student exists before enrolling them?

### Synchronous WebClients
We built `StudentClient` and `CourseClient` utilizing Spring Webflux's `WebClient`. 
When the frontend asks to enroll a student via CNIE, the logic works sequentially over the network:

### The Business Logic Code
Inside `EnrollmentService.java`, here is the absolute core layout of our flow. Notice the hardcoded limits (`MAX_STUDENTS_PER_COURSE`) and how exceptions naturally exit the flow.

```java
// EnrollmentService.java
private static final int MAX_STUDENTS_PER_COURSE = 3;
private static final int CANCELLATION_WINDOW_HOURS = 24;

public EnrollmentResponseDTO enrollStudent(EnrollmentRequest request) {
    // 1. Ask Student Service if the CNIE exists via HTTP
    StudentDTO student = studentClient.getStudentByCnie(request.getStudentCnie());
    
    // 2. Ask Course Service if the Course ID exists via HTTP
    CourseDTO course = courseClient.getCourseById(request.getCourseId());

    // 3. Apply Course Limit Rules
    long currentCount = enrollmentRepository.countByCourseId(request.getCourseId());
    if (currentCount >= MAX_STUDENTS_PER_COURSE) {
        throw new IllegalArgumentException(
            "Course '" + course.getTitle() + "' is full (max " + MAX_STUDENTS_PER_COURSE + " students)."
        );
    }

    // 4. Prevent Duplication
    if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), request.getCourseId())) {
        throw new IllegalArgumentException("Student is already enrolled in '" + course.getTitle() + "'.");
    }

    // 5. Persist mapping
    Enrollment enrollment = Enrollment.builder()
            .studentId(student.getId())
            .courseId(course.getId())
            .enrollmentDate(LocalDateTime.now())
            .build();
            
    return buildResponseDTO(enrollmentRepository.save(enrollment), student.getCnie(), course);
}
```

### Time-Based Constraint Logic
If a student wants to drop a course, they have a **24 hour cancellation window**. 
```java
public void cancelEnrollment(Long enrollmentId) {
    Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow(...);

    // Business Rule Check
    if (!isDeletable(enrollment.getEnrollmentDate())) {
        throw new IllegalArgumentException(
                "Cancellation window has passed. Enrollments can only be cancelled within 24 hours.");
    }
    enrollmentRepository.deleteById(enrollmentId);
}

private boolean isDeletable(LocalDateTime enrollmentDate) {
    return LocalDateTime.now().isBefore(enrollmentDate.plusHours(24));
}
```

---

## 5. Global Exception Handling

Because we map standard Java `IllegalArgumentException` objects above, we don't want the user to see massive Java stack traces when the course is full! 
To intercept this cleanly, every service utilizes an `@ControllerAdvice`:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
```
*Because of this class, if the course limit of 3 is hit, the frontend receives a clean `{"error": "Course is full..."}` JSON payload with an HTTP 400 status.*

---

## 6. The Angular Setup (Frontend)

The frontend is built on **Angular 18** using modern configuration (Standalone Components, SCSS modules).

### UI Layout Structuring
The UI was meticulously architected using deep CSS standards, prioritizing `flex` properties over clunky bootstrap frameworks.

For instance, the `/enroll` page uses dynamic flex-flow to correctly center the forms on any monitor:
```scss
// enroll.scss
.enroll-layout {
  max-width: 1000px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: row;        // Keep form and success-card side-by-side
  justify-content: center;    // Dead center on the screen
  gap: 3rem;
  align-items: flex-start;    // Ensures tops of both cards perfectly align

  @media (max-width: 800px) { 
    flex-direction: column;   // On mobile phones, stack them vertically
    align-items: center;
  }
}
```

### Component Flow (`enroll.ts`)
The `EnrollComponent` glues our APIs perfectly via Observables natively supported in Angular.
1. `ngOnInit` triggers the service `courseService.getAllCourses()`.
2. Dropdown binds (`[(ngModel)]="selectedCourseId"`) values from that payload.
3. The `enroll()` method bridges to the backend via POST:

```typescript
// enroll.ts
this.enrollSub = this.enrollmentService.enrollStudent({
  studentCnie: this.studentCnie,
  courseId: this.selectedCourseId
}).subscribe({
  next: (res) => {
    this.success = `Successfully enrolled in "${course.title}"!`;
    this.result = res; // Renders the stylish right-side Success Card with the checkmark
    this.loading = false;
  },
  error: (err) => {
    // Correctly parses the HTTP 400 Bad Request if the Course is Full or Rule Broken
    this.error = err.error?.error || 'Failed to enroll student';
    this.loading = false;
  }
});
```

Because of this seamless integration between the Spring exception handler and the Angular `Observable.error` block, our complex business rules from the `enrollment-service` (limit 3, no duplicates) appear directly inside the UI as beautiful red inline alerts without forcing manual API validation on the Javascript level.

---
*Built from ground up mapping theoretical microservice domains to robust business limits perfectly integrated across a reactive full-stack flow.*
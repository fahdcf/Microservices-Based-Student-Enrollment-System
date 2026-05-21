# Student Enrollment Microservices — Full Build Tasks

> **Stack**: Java 21 · Spring Boot 3.4 · Spring Cloud 2024 · PostgreSQL · Angular 18 · Angular Material + custom SCSS  
> **Architecture**: 4 Spring Boot apps (student-service, course-service, enrollment-service, api-gateway) + 1 Angular SPA  
> **Execution**: Run tasks top-to-bottom. Each task is self-contained and testable before moving on.

---

## PHASE 0 — Workspace Bootstrap

### Task 0.1 — Create root project folder

```
mkdir student-enrollment-system
cd student-enrollment-system
mkdir student-service course-service enrollment-service api-gateway frontend
```

Create a root `README.md`:

```markdown
# Student Enrollment System
Microservices-based student enrollment built with Spring Boot + Angular.

## Services
| Service            | Port |
|--------------------|------|
| student-service    | 8081 |
| course-service     | 8082 |
| enrollment-service | 8083 |
| api-gateway        | 8080 |
| Angular frontend   | 4200 |
```

---

## PHASE 1 — Student Service

### Task 1.1 — Generate Spring Boot project

Go to https://start.spring.io and generate with:

- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.4.x
- **Group**: `com.enrollment`
- **Artifact**: `student-service`
- **Packaging**: Jar
- **Java**: 21
- **Dependencies**:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Validation
  - Lombok
  - Spring Boot DevTools

Extract into `student-enrollment-system/student-service/`.

---

### Task 1.2 — Create PostgreSQL databases

Unlike MySQL, PostgreSQL does **not** auto-create databases. Run these commands once before starting any service:

```bash
psql -U postgres -c "CREATE DATABASE student_db;"
psql -U postgres -c "CREATE DATABASE course_db;"
psql -U postgres -c "CREATE DATABASE enrollment_db;"
```

Or via psql shell:

```sql
CREATE DATABASE student_db;
CREATE DATABASE course_db;
CREATE DATABASE enrollment_db;
```

Hibernate will create all tables automatically on first boot via `ddl-auto=update`.

---

### Task 1.3 — Configure `application.properties`

File: `src/main/resources/application.properties`

```properties
spring.application.name=student-service
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5432/student_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

---

### Task 1.4 — Create the `Student` entity

File: `src/main/java/com/enrollment/student/entity/Student.java`

```java
package com.enrollment.student.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "CNIE is required")
    @Pattern(regexp = "^[A-Z]{1,2}[0-9]{4,8}$", message = "CNIE must match format e.g. CD2387")
    @Column(unique = true, nullable = false)
    private String cnie;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(unique = true, nullable = false)
    private String email;
}
```

---

### Task 1.5 — Create the `StudentRepository`

File: `src/main/java/com/enrollment/student/repository/StudentRepository.java`

```java
package com.enrollment.student.repository;

import com.enrollment.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByCnie(String cnie);
    boolean existsByCnie(String cnie);
    boolean existsByEmail(String email);
}
```

---

### Task 1.6 — Create DTOs

File: `src/main/java/com/enrollment/student/dto/StudentRequest.java`

```java
package com.enrollment.student.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StudentRequest {

    @NotBlank(message = "CNIE is required")
    @Pattern(regexp = "^[A-Z]{1,2}[0-9]{4,8}$", message = "CNIE must match format e.g. CD2387")
    private String cnie;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
}
```

File: `src/main/java/com/enrollment/student/dto/StudentResponse.java`

```java
package com.enrollment.student.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentResponse {
    private Long id;
    private String cnie;
    private String firstName;
    private String lastName;
    private String email;
}
```

---

### Task 1.7 — Create `StudentMapper`

File: `src/main/java/com/enrollment/student/mapper/StudentMapper.java`

```java
package com.enrollment.student.mapper;

import com.enrollment.student.dto.*;
import com.enrollment.student.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public Student toEntity(StudentRequest request) {
        return Student.builder()
                .cnie(request.getCnie())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();
    }

    public StudentResponse toResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .cnie(student.getCnie())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .build();
    }
}
```

---

### Task 1.8 — Create `StudentService`

File: `src/main/java/com/enrollment/student/service/StudentService.java`

```java
package com.enrollment.student.service;

import com.enrollment.student.dto.*;
import com.enrollment.student.entity.Student;
import com.enrollment.student.mapper.StudentMapper;
import com.enrollment.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByCnie(request.getCnie())) {
            throw new IllegalArgumentException("A student with CNIE " + request.getCnie() + " already exists.");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A student with this email already exists.");
        }
        Student saved = studentRepository.save(studentMapper.toEntity(request));
        return studentMapper.toResponse(saved);
    }

    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        return studentMapper.toResponse(student);
    }

    public StudentResponse getStudentByCnie(String cnie) {
        Student student = studentRepository.findByCnie(cnie)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with CNIE: " + cnie));
        return studentMapper.toResponse(student);
    }

    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new IllegalArgumentException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }
}
```

---

### Task 1.9 — Create `StudentController`

File: `src/main/java/com/enrollment/student/controller/StudentController.java`

```java
package com.enrollment.student.controller;

import com.enrollment.student.dto.*;
import com.enrollment.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAll() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/cnie/{cnie}")
    public ResponseEntity<StudentResponse> getByCnie(@PathVariable String cnie) {
        return ResponseEntity.ok(studentService.getStudentByCnie(cnie));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

### Task 1.10 — Create Global Exception Handler

File: `src/main/java/com/enrollment/student/exception/GlobalExceptionHandler.java`

```java
package com.enrollment.student.exception;

import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }
}
```

---

### Task 1.11 — Add CORS configuration

File: `src/main/java/com/enrollment/student/config/CorsConfig.java`

```java
package com.enrollment.student.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200", "http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

---

### Task 1.12 — Test student-service

Start the service and verify with curl or Postman:

```bash
# Create student
curl -X POST http://localhost:8081/api/students \
  -H "Content-Type: application/json" \
  -d '{"cnie":"CD2387","firstName":"Fahd","lastName":"Chafai","email":"fahd@test.com"}'

# Get all
curl http://localhost:8081/api/students

# Get by CNIE
curl http://localhost:8081/api/students/cnie/CD2387
```

Expected: 201 Created with student JSON, 200 OK with list.

> **Note**: Make sure `student_db` exists in PostgreSQL before starting. Tables are auto-created by Hibernate.

---

## PHASE 2 — Course Service

### Task 2.1 — Generate Spring Boot project

Same setup as student-service at https://start.spring.io:

- **Artifact**: `course-service`
- **Same dependencies** as student-service (PostgreSQL Driver instead of MySQL Driver)

Extract into `student-enrollment-system/course-service/`.

---

### Task 2.2 — Configure `application.properties`

```properties
spring.application.name=course-service
server.port=8082

spring.datasource.url=jdbc:postgresql://localhost:5432/course_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

### Task 2.3 — Create `Course` entity

File: `src/main/java/com/enrollment/course/entity/Course.java`

```java
package com.enrollment.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 10, message = "Credits cannot exceed 10")
    private int credits;
}
```

---

### Task 2.4 — Create `CourseRepository`

File: `src/main/java/com/enrollment/course/repository/CourseRepository.java`

```java
package com.enrollment.course.repository;

import com.enrollment.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByTitle(String title);
}
```

---

### Task 2.5 — Create DTOs

File: `src/main/java/com/enrollment/course/dto/CourseRequest.java`

```java
package com.enrollment.course.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Course title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Min(1) @Max(10)
    private int credits;
}
```

File: `src/main/java/com/enrollment/course/dto/CourseResponse.java`

```java
package com.enrollment.course.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private int credits;
}
```

---

### Task 2.6 — Create `CourseMapper`

File: `src/main/java/com/enrollment/course/mapper/CourseMapper.java`

```java
package com.enrollment.course.mapper;

import com.enrollment.course.dto.*;
import com.enrollment.course.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public Course toEntity(CourseRequest request) {
        return Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .credits(request.getCredits())
                .build();
    }

    public CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .credits(course.getCredits())
                .build();
    }
}
```

---

### Task 2.7 — Create `CourseService`

File: `src/main/java/com/enrollment/course/service/CourseService.java`

```java
package com.enrollment.course.service;

import com.enrollment.course.dto.*;
import com.enrollment.course.entity.Course;
import com.enrollment.course.mapper.CourseMapper;
import com.enrollment.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByTitle(request.getTitle())) {
            throw new IllegalArgumentException("A course with title '" + request.getTitle() + "' already exists.");
        }
        Course saved = courseRepository.save(courseMapper.toEntity(request));
        return courseMapper.toResponse(saved);
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + id));
        return courseMapper.toResponse(course);
    }

    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + id));
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        return courseMapper.toResponse(courseRepository.save(course));
    }

    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }
}
```

---

### Task 2.8 — Create `CourseController`

File: `src/main/java/com/enrollment/course/controller/CourseController.java`

```java
package com.enrollment.course.controller;

import com.enrollment.course.dto.*;
import com.enrollment.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAll() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

### Task 2.9 — Add CORS config and Global Exception Handler

Copy `CorsConfig.java` and `GlobalExceptionHandler.java` from student-service, adjusting the package to `com.enrollment.course.config` and `com.enrollment.course.exception`.

---

### Task 2.10 — Test course-service

```bash
curl -X POST http://localhost:8082/api/courses \
  -H "Content-Type: application/json" \
  -d '{"title":"Spring Framework","description":"Learn Spring Boot microservices","credits":4}'

curl http://localhost:8082/api/courses
```

> **Note**: Make sure `course_db` exists in PostgreSQL before starting.

---

## PHASE 3 — Enrollment Service

### Task 3.1 — Generate Spring Boot project

Go to https://start.spring.io:

- **Artifact**: `enrollment-service`
- **Dependencies**:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Validation
  - Lombok
  - Spring Boot DevTools
  - Spring Reactive Web *(for WebClient)*

Extract into `student-enrollment-system/enrollment-service/`.

---

### Task 3.2 — Configure `application.properties`

```properties
spring.application.name=enrollment-service
server.port=8083

spring.datasource.url=jdbc:postgresql://localhost:5432/enrollment_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Internal service URLs
student.service.url=http://localhost:8081
course.service.url=http://localhost:8082
```

---

### Task 3.3 — Create `Enrollment` entity

File: `src/main/java/com/enrollment/enrollment/entity/Enrollment.java`

```java
package com.enrollment.enrollment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"studentId", "courseId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDateTime enrollmentDate;
}
```

---

### Task 3.4 — Create `EnrollmentRepository`

File: `src/main/java/com/enrollment/enrollment/repository/EnrollmentRepository.java`

```java
package com.enrollment.enrollment.repository;

import com.enrollment.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    long countByCourseId(Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
```

---

### Task 3.5 — Create all DTOs

File: `src/main/java/com/enrollment/enrollment/dto/EnrollmentRequest.java`

```java
package com.enrollment.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EnrollmentRequest {

    @NotBlank(message = "Student CNIE is required")
    private String studentCnie;

    @NotNull(message = "Course ID is required")
    private Long courseId;
}
```

File: `src/main/java/com/enrollment/enrollment/dto/EnrollmentResponseDTO.java`

```java
package com.enrollment.enrollment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentResponseDTO {
    private Long enrollmentId;
    private String studentCnie;
    private String courseName;
    private int courseCredits;
    private LocalDateTime enrollmentDate;
    private boolean deletable;
}
```

File: `src/main/java/com/enrollment/enrollment/dto/StudentDTO.java`

```java
package com.enrollment.enrollment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String cnie;
    private String firstName;
    private String lastName;
    private String email;
}
```

File: `src/main/java/com/enrollment/enrollment/dto/CourseDTO.java`

```java
package com.enrollment.enrollment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private int credits;
}
```

---

### Task 3.6 — Create WebClient configuration

File: `src/main/java/com/enrollment/enrollment/config/WebClientConfig.java`

```java
package com.enrollment.enrollment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${student.service.url}")
    private String studentServiceUrl;

    @Value("${course.service.url}")
    private String courseServiceUrl;

    @Bean
    public WebClient studentWebClient() {
        return WebClient.builder()
                .baseUrl(studentServiceUrl)
                .build();
    }

    @Bean
    public WebClient courseWebClient() {
        return WebClient.builder()
                .baseUrl(courseServiceUrl)
                .build();
    }
}
```

---

### Task 3.7 — Create service clients

File: `src/main/java/com/enrollment/enrollment/client/StudentClient.java`

```java
package com.enrollment.enrollment.client;

import com.enrollment.enrollment.dto.StudentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StudentClient {

    @Qualifier("studentWebClient")
    private final WebClient studentWebClient;

    public StudentDTO getStudentByCnie(String cnie) {
        return studentWebClient.get()
                .uri("/api/students/cnie/{cnie}", cnie)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Student not found with CNIE: " + cnie)))
                .bodyToMono(StudentDTO.class)
                .block();
    }

    public StudentDTO getStudentById(Long id) {
        return studentWebClient.get()
                .uri("/api/students/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Student not found with id: " + id)))
                .bodyToMono(StudentDTO.class)
                .block();
    }
}
```

File: `src/main/java/com/enrollment/enrollment/client/CourseClient.java`

```java
package com.enrollment.enrollment.client;

import com.enrollment.enrollment.dto.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CourseClient {

    @Qualifier("courseWebClient")
    private final WebClient courseWebClient;

    public CourseDTO getCourseById(Long id) {
        return courseWebClient.get()
                .uri("/api/courses/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Course not found with id: " + id)))
                .bodyToMono(CourseDTO.class)
                .block();
    }
}
```

---

### Task 3.8 — Create `EnrollmentService` (the brain)

File: `src/main/java/com/enrollment/enrollment/service/EnrollmentService.java`

```java
package com.enrollment.enrollment.service;

import com.enrollment.enrollment.client.*;
import com.enrollment.enrollment.dto.*;
import com.enrollment.enrollment.entity.Enrollment;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final int MAX_STUDENTS_PER_COURSE = 3;
    private static final int CANCELLATION_WINDOW_HOURS = 24;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentClient studentClient;
    private final CourseClient courseClient;

    public EnrollmentResponseDTO enrollStudent(EnrollmentRequest request) {
        // 1. Verify student exists via Student Service
        StudentDTO student = studentClient.getStudentByCnie(request.getStudentCnie());

        // 2. Verify course exists via Course Service
        CourseDTO course = courseClient.getCourseById(request.getCourseId());

        // 3. Check 3-student cap
        long currentCount = enrollmentRepository.countByCourseId(request.getCourseId());
        if (currentCount >= MAX_STUDENTS_PER_COURSE) {
            throw new IllegalArgumentException(
                    "Course '" + course.getTitle() + "' is full (max " + MAX_STUDENTS_PER_COURSE + " students).");
        }

        // 4. Check for duplicate enrollment
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), request.getCourseId())) {
            throw new IllegalArgumentException(
                    "Student is already enrolled in '" + course.getTitle() + "'.");
        }

        // 5. Save enrollment
        Enrollment enrollment = Enrollment.builder()
                .studentId(student.getId())
                .courseId(course.getId())
                .enrollmentDate(LocalDateTime.now())
                .build();
        Enrollment saved = enrollmentRepository.save(enrollment);

        return buildResponseDTO(saved, student.getCnie(), course);
    }

    public List<EnrollmentResponseDTO> getEnrollmentsByStudentCnie(String cnie) {
        StudentDTO student = studentClient.getStudentByCnie(cnie);
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        return enrollments.stream().map(enrollment -> {
            CourseDTO course = courseClient.getCourseById(enrollment.getCourseId());
            return buildResponseDTO(enrollment, student.getCnie(), course);
        }).toList();
    }

    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + enrollmentId));

        if (!isDeletable(enrollment.getEnrollmentDate())) {
            throw new IllegalArgumentException(
                    "Cancellation window has passed. Enrollments can only be cancelled within "
                    + CANCELLATION_WINDOW_HOURS + " hours.");
        }

        enrollmentRepository.deleteById(enrollmentId);
    }

    private boolean isDeletable(LocalDateTime enrollmentDate) {
        return LocalDateTime.now().isBefore(enrollmentDate.plusHours(CANCELLATION_WINDOW_HOURS));
    }

    private EnrollmentResponseDTO buildResponseDTO(Enrollment enrollment, String cnie, CourseDTO course) {
        return EnrollmentResponseDTO.builder()
                .enrollmentId(enrollment.getId())
                .studentCnie(cnie)
                .courseName(course.getTitle())
                .courseCredits(course.getCredits())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .deletable(isDeletable(enrollment.getEnrollmentDate()))
                .build();
    }
}
```

---

### Task 3.9 — Create `EnrollmentController`

File: `src/main/java/com/enrollment/enrollment/controller/EnrollmentController.java`

```java
package com.enrollment.enrollment.controller;

import com.enrollment.enrollment.dto.*;
import com.enrollment.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponseDTO> enroll(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudent(request));
    }

    @GetMapping("/student/{cnie}")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByStudentCnie(@PathVariable String cnie) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudentCnie(cnie));
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<Void> cancel(@PathVariable Long enrollmentId) {
        enrollmentService.cancelEnrollment(enrollmentId);
        return ResponseEntity.noContent().build();
    }
}
```

---

### Task 3.10 — Add CORS config and Global Exception Handler

Copy `CorsConfig.java` from student-service, package `com.enrollment.enrollment.config`.  
Copy `GlobalExceptionHandler.java`, package `com.enrollment.enrollment.exception`.

---

### Task 3.11 — Test enrollment-service

Make sure student-service (8081) and course-service (8082) are running first, and `enrollment_db` exists in PostgreSQL.

```bash
# Enroll student
curl -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentCnie":"CD2387","courseId":1}'

# Get student enrollments
curl http://localhost:8083/api/enrollments/student/CD2387

# Cancel enrollment
curl -X DELETE http://localhost:8083/api/enrollments/1
```

Expected: combined JSON with courseName, studentCnie, deletable flag.

---

## PHASE 4 — API Gateway

### Task 4.1 — Generate Spring Boot project

Go to https://start.spring.io:

- **Artifact**: `api-gateway`
- **Dependencies**:
  - **Gateway** (Spring Cloud Gateway — reactive)
  - Lombok
  - Spring Boot DevTools
  - **Spring Cloud** — add `spring-cloud-dependencies` BOM in pom.xml

Extract into `student-enrollment-system/api-gateway/`.

---

### Task 4.2 — Add Spring Cloud BOM to `pom.xml`

Inside `<dependencyManagement>`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2024.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Ensure gateway dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

---

### Task 4.3 — Configure `application.yml`

Replace `application.properties` with `application.yml`:

```yaml
spring:
  application:
    name: api-gateway
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

      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:4200"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: false

server:
  port: 8080
```

---

### Task 4.4 — Test API Gateway

Start all 4 services. Every request through port 8080 should route correctly:

```bash
curl http://localhost:8080/api/students
curl http://localhost:8080/api/courses
curl http://localhost:8080/api/enrollments/student/CD2387
```

---

## PHASE 5 — Angular Frontend

### Task 5.1 — Scaffold Angular project

```bash
cd student-enrollment-system/frontend
npm install -g @angular/cli
ng new enrollment-app --routing=true --style=scss --standalone=true
cd enrollment-app
ng add @angular/material
```

When prompted for Angular Material theme: choose **Custom** (we will override it).

Install additional packages:

```bash
npm install @angular/animations
```

---

### Task 5.2 — Configure `environment.ts`

File: `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

---

### Task 5.3 — Global SCSS design system

File: `src/styles.scss`

```scss
@use '@angular/material' as mat;

// ── Design Tokens ────────────────────────────────────────────────
:root {
  --bg:           #0A0E1A;
  --surface:      #111827;
  --surface-2:    #1a2235;
  --border:       #1E2D40;
  --accent:       #6C63FF;
  --accent-soft:  rgba(108, 99, 255, 0.15);
  --accent-2:     #00D4AA;
  --danger:       #FF4757;
  --warn:         #FFA502;
  --text:         #F1F5F9;
  --muted:        #64748B;
  --radius:       12px;
  --radius-lg:    20px;
  --shadow:       0 4px 24px rgba(0, 0, 0, 0.4);
  --shadow-glow:  0 0 32px rgba(108, 99, 255, 0.25);
  --font-display: 'Syne', sans-serif;
  --font-body:    'DM Sans', sans-serif;
}

// ── Google Fonts ────────────────────────────────────────────────
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@600;700;800&family=DM+Sans:wght@300;400;500&display=swap');

// ── Reset & Base ─────────────────────────────────────────────────
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

html, body {
  height: 100%;
  background: var(--bg);
  color: var(--text);
  font-family: var(--font-body);
  font-size: 16px;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}

h1, h2, h3, h4 {
  font-family: var(--font-display);
  line-height: 1.2;
}

// ── Utility classes ───────────────────────────────────────────────
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 2rem;
  box-shadow: var(--shadow);
  transition: box-shadow 0.3s ease, border-color 0.3s ease;

  &:hover { border-color: var(--accent); box-shadow: var(--shadow-glow); }
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  font-family: var(--font-body);

  &.badge-accent  { background: var(--accent-soft); color: var(--accent); border: 1px solid rgba(108,99,255,0.3); }
  &.badge-success { background: rgba(0,212,170,0.12); color: var(--accent-2); border: 1px solid rgba(0,212,170,0.3); }
  &.badge-danger  { background: rgba(255,71,87,0.12); color: var(--danger); border: 1px solid rgba(255,71,87,0.3); }
  &.badge-warn    { background: rgba(255,165,2,0.12); color: var(--warn); border: 1px solid rgba(255,165,2,0.3); }
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: var(--radius);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;

  &.btn-primary {
    background: var(--accent);
    color: #fff;
    &:hover { background: #5a52e0; transform: translateY(-1px); box-shadow: var(--shadow-glow); }
  }
  &.btn-outline {
    background: transparent;
    color: var(--text);
    border: 1px solid var(--border);
    &:hover { border-color: var(--accent); color: var(--accent); }
  }
  &.btn-danger {
    background: rgba(255,71,87,0.12);
    color: var(--danger);
    border: 1px solid rgba(255,71,87,0.3);
    &:hover { background: var(--danger); color: #fff; }
  }
  &:disabled { opacity: 0.4; cursor: not-allowed; transform: none !important; }
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    font-size: 0.8rem;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 500;
  }

  input, select, textarea {
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    color: var(--text);
    padding: 12px 16px;
    font-family: var(--font-body);
    font-size: 0.9rem;
    transition: border-color 0.2s;
    outline: none;
    width: 100%;

    &:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
    &::placeholder { color: var(--muted); }
  }

  .error-msg {
    font-size: 0.78rem;
    color: var(--danger);
  }
}

.page-header {
  margin-bottom: 2.5rem;

  .page-title {
    font-size: 2rem;
    font-weight: 800;
    background: linear-gradient(135deg, var(--text) 0%, var(--accent) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .page-subtitle {
    color: var(--muted);
    margin-top: 4px;
    font-size: 0.95rem;
  }
}

.alert {
  padding: 14px 18px;
  border-radius: var(--radius);
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 10px;

  &.alert-error   { background: rgba(255,71,87,0.1); border: 1px solid rgba(255,71,87,0.3); color: #ff6b7a; }
  &.alert-success { background: rgba(0,212,170,0.1); border: 1px solid rgba(0,212,170,0.3); color: var(--accent-2); }
}

.spinner-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 3rem;
}

// ── Scrollbar ────────────────────────────────────────────────────
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: var(--bg); }
::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: var(--accent); }
```

---

### Task 5.4 — Create API models (interfaces)

File: `src/app/models/student.model.ts`

```typescript
export interface Student {
  id: number;
  cnie: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface StudentRequest {
  cnie: string;
  firstName: string;
  lastName: string;
  email: string;
}
```

File: `src/app/models/course.model.ts`

```typescript
export interface Course {
  id: number;
  title: string;
  description: string;
  credits: number;
}

export interface CourseRequest {
  title: string;
  description: string;
  credits: number;
}
```

File: `src/app/models/enrollment.model.ts`

```typescript
export interface Enrollment {
  enrollmentId: number;
  studentCnie: string;
  courseName: string;
  courseCredits: number;
  enrollmentDate: string;
  deletable: boolean;
}

export interface EnrollmentRequest {
  studentCnie: string;
  courseId: number;
}
```

---

### Task 5.5 — Create API services

File: `src/app/services/student.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Student, StudentRequest } from '../models/student.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly url = `${environment.apiUrl}/students`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Student[]> {
    return this.http.get<Student[]>(this.url);
  }

  getByCnie(cnie: string): Observable<Student> {
    return this.http.get<Student>(`${this.url}/cnie/${cnie}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.url, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
```

File: `src/app/services/course.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Course, CourseRequest } from '../models/course.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private readonly url = `${environment.apiUrl}/courses`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Course[]> {
    return this.http.get<Course[]>(this.url);
  }

  create(request: CourseRequest): Observable<Course> {
    return this.http.post<Course>(this.url, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
```

File: `src/app/services/enrollment.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Enrollment, EnrollmentRequest } from '../models/enrollment.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly url = `${environment.apiUrl}/enrollments`;

  constructor(private http: HttpClient) {}

  enroll(request: EnrollmentRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(this.url, request);
  }

  getByStudentCnie(cnie: string): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${this.url}/student/${cnie}`);
  }

  cancel(enrollmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${enrollmentId}`);
  }
}
```

---

### Task 5.6 — Setup Angular routing and app shell

File: `src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'students',
    loadComponent: () => import('./pages/students/students.component').then(m => m.StudentsComponent)
  },
  {
    path: 'courses',
    loadComponent: () => import('./pages/courses/courses.component').then(m => m.CoursesComponent)
  },
  {
    path: 'enroll',
    loadComponent: () => import('./pages/enroll/enroll.component').then(m => m.EnrollComponent)
  },
  { path: '**', redirectTo: '/dashboard' }
];
```

File: `src/app/app.config.ts`

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideAnimations()
  ]
};
```

---

### Task 5.7 — Build the NavBar component

Generate: `ng g c components/navbar --standalone`

File: `src/app/components/navbar/navbar.component.ts`

```typescript
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '⬡' },
    { path: '/students',  label: 'Students',  icon: '◈' },
    { path: '/courses',   label: 'Courses',   icon: '◇' },
    { path: '/enroll',    label: 'Enroll',    icon: '◉' },
  ];
}
```

File: `src/app/components/navbar/navbar.component.html`

```html
<nav class="navbar">
  <div class="nav-brand">
    <span class="brand-icon">⬡</span>
    <span class="brand-name">EduEnroll</span>
  </div>
  <div class="nav-links">
    <a
      *ngFor="let item of navItems"
      [routerLink]="item.path"
      routerLinkActive="active"
      class="nav-link"
    >
      <span class="nav-icon">{{ item.icon }}</span>
      <span>{{ item.label }}</span>
    </a>
  </div>
</nav>
```

File: `src/app/components/navbar/navbar.component.scss`

```scss
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2.5rem;
  height: 64px;
  background: rgba(10, 14, 26, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border);

  .nav-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
    color: var(--text);

    .brand-icon {
      font-size: 1.5rem;
      color: var(--accent);
    }
    .brand-name {
      font-family: var(--font-display);
      font-size: 1.2rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
  }

  .nav-links {
    display: flex;
    gap: 4px;
  }

  .nav-link {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border-radius: var(--radius);
    text-decoration: none;
    color: var(--muted);
    font-size: 0.875rem;
    transition: all 0.2s ease;

    .nav-icon { font-size: 0.9rem; }

    &:hover { color: var(--text); background: var(--surface); }
    &.active { color: var(--accent); background: var(--accent-soft); }
  }
}
```

---

### Task 5.8 — Build the App root component

File: `src/app/app.component.html`

```html
<app-navbar></app-navbar>
<main class="main-content">
  <router-outlet></router-outlet>
</main>
```

File: `src/app/app.component.scss`

```scss
.main-content {
  padding-top: 64px;
  min-height: 100vh;
}
```

File: `src/app/app.component.ts`

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {}
```

---

### Task 5.9 — Build Dashboard page

Generate: `ng g c pages/dashboard --standalone`

File: `src/app/pages/dashboard/dashboard.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StudentService } from '../../services/student.service';
import { EnrollmentService } from '../../services/enrollment.service';
import { CourseService } from '../../services/course.service';
import { Enrollment } from '../../models/enrollment.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  totalStudents = 0;
  totalCourses = 0;

  searchCnie = '';
  enrollments: Enrollment[] = [];
  loading = false;
  error = '';
  searched = false;

  constructor(
    private studentService: StudentService,
    private enrollmentService: EnrollmentService,
    private courseService: CourseService
  ) {}

  ngOnInit() {
    this.studentService.getAll().subscribe(s => this.totalStudents = s.length);
    this.courseService.getAll().subscribe(c => this.totalCourses = c.length);
  }

  search() {
    if (!this.searchCnie.trim()) return;
    this.loading = true;
    this.error = '';
    this.searched = false;

    this.enrollmentService.getByStudentCnie(this.searchCnie.trim()).subscribe({
      next: (data) => {
        this.enrollments = data;
        this.searched = true;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Student not found.';
        this.enrollments = [];
        this.searched = true;
        this.loading = false;
      }
    });
  }

  cancel(enrollmentId: number) {
    this.enrollmentService.cancel(enrollmentId).subscribe({
      next: () => {
        this.enrollments = this.enrollments.filter(e => e.enrollmentId !== enrollmentId);
      },
      error: (err) => {
        this.error = err.error?.error || 'Cannot cancel enrollment.';
      }
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('fr-MA', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
```

File: `src/app/pages/dashboard/dashboard.component.html`

```html
<div class="dashboard-page">

  <!-- Hero Stats -->
  <section class="hero">
    <div class="hero-bg"></div>
    <div class="hero-content">
      <h1 class="hero-title">Student Dashboard</h1>
      <p class="hero-sub">Track enrollments, manage your academic journey</p>
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">{{ totalStudents }}</div>
          <div class="stat-label">Total Students</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ totalCourses }}</div>
          <div class="stat-label">Available Courses</div>
        </div>
      </div>
    </div>
  </section>

  <!-- Lookup -->
  <section class="lookup-section">
    <div class="lookup-card card">
      <div class="page-header">
        <div class="page-title">My Enrollments</div>
        <div class="page-subtitle">Enter your CNIE to view your enrolled courses</div>
      </div>

      <div class="search-row">
        <div class="form-field" style="flex:1">
          <label>Student CNIE</label>
          <input
            type="text"
            [(ngModel)]="searchCnie"
            placeholder="e.g. CD2387"
            (keyup.enter)="search()"
          />
        </div>
        <button class="btn btn-primary search-btn" (click)="search()" [disabled]="loading">
          <span *ngIf="!loading">Search</span>
          <span *ngIf="loading">Loading...</span>
        </button>
      </div>

      <div *ngIf="error" class="alert alert-error" style="margin-top:1rem">⚠ {{ error }}</div>

      <!-- Results -->
      <div *ngIf="searched && !loading" class="results">
        <div *ngIf="enrollments.length === 0 && !error" class="empty-state">
          <div class="empty-icon">◇</div>
          <p>No enrollments found for <strong>{{ searchCnie }}</strong></p>
        </div>

        <div *ngIf="enrollments.length > 0" class="enrollments-grid">
          <div *ngFor="let e of enrollments" class="enrollment-card">
            <div class="ec-header">
              <div class="ec-course">{{ e.courseName }}</div>
              <span [class]="e.deletable ? 'badge badge-success' : 'badge badge-danger'">
                {{ e.deletable ? '✓ Cancellable' : '✗ Locked' }}
              </span>
            </div>
            <div class="ec-meta">
              <span class="badge badge-accent">{{ e.courseCredits }} credits</span>
              <span class="ec-date">{{ formatDate(e.enrollmentDate) }}</span>
            </div>
            <button
              class="btn btn-danger ec-cancel"
              [disabled]="!e.deletable"
              (click)="cancel(e.enrollmentId)"
            >
              Cancel Enrollment
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</div>
```

File: `src/app/pages/dashboard/dashboard.component.scss`

```scss
.dashboard-page { padding-bottom: 4rem; }

.hero {
  position: relative;
  padding: 5rem 2.5rem 4rem;
  overflow: hidden;
  text-align: center;

  .hero-bg {
    position: absolute; inset: 0;
    background: radial-gradient(ellipse 80% 60% at 50% 0%, rgba(108,99,255,0.18) 0%, transparent 70%);
    pointer-events: none;
  }

  .hero-content { position: relative; max-width: 700px; margin: 0 auto; }

  .hero-title {
    font-size: clamp(2rem, 5vw, 3.5rem);
    font-weight: 800;
    background: linear-gradient(135deg, #fff 0%, var(--accent) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .hero-sub { color: var(--muted); margin-top: 0.5rem; font-size: 1rem; }
}

.stats-row {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  margin-top: 2.5rem;
  flex-wrap: wrap;
}

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 1.5rem 3rem;
  text-align: center;
  min-width: 160px;
  transition: all 0.3s;
  &:hover { border-color: var(--accent); box-shadow: var(--shadow-glow); }

  .stat-value { font-family: var(--font-display); font-size: 2.5rem; font-weight: 800; color: var(--accent); }
  .stat-label { color: var(--muted); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.08em; margin-top: 4px; }
}

.lookup-section { padding: 0 2.5rem; max-width: 900px; margin: 0 auto; }

.search-row {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  .search-btn { height: 46px; white-space: nowrap; }
}

.results { margin-top: 2rem; }

.empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--muted);
  .empty-icon { font-size: 3rem; margin-bottom: 1rem; opacity: 0.3; }
}

.enrollments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.25rem;
}

.enrollment-card {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: all 0.25s;
  &:hover { border-color: var(--accent); }

  .ec-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
  }
  .ec-course { font-family: var(--font-display); font-size: 1.05rem; font-weight: 700; }
  .ec-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
  .ec-date { font-size: 0.8rem; color: var(--muted); }
  .ec-cancel { align-self: flex-start; }
}
```

---

### Task 5.10 — Build Students page

Generate: `ng g c pages/students --standalone`

File: `src/app/pages/students/students.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { StudentService } from '../../services/student.service';
import { Student, StudentRequest } from '../../models/student.model';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './students.component.html',
  styleUrls: ['./students.component.scss']
})
export class StudentsComponent implements OnInit {
  students: Student[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;

  formData: StudentRequest = { cnie: '', firstName: '', lastName: '', email: '' };

  constructor(private studentService: StudentService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.studentService.getAll().subscribe({
      next: (data) => { this.students = data; this.loading = false; },
      error: () => { this.error = 'Failed to load students.'; this.loading = false; }
    });
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.studentService.create(this.formData).subscribe({
      next: () => {
        this.success = 'Student registered successfully!';
        this.error = '';
        this.showForm = false;
        form.reset();
        this.formData = { cnie: '', firstName: '', lastName: '', email: '' };
        this.load();
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to create student.';
        this.success = '';
      }
    });
  }

  delete(id: number) {
    if (!confirm('Delete this student?')) return;
    this.studentService.delete(id).subscribe({
      next: () => this.load(),
      error: (err) => this.error = err.error?.error || 'Cannot delete student.'
    });
  }
}
```

File: `src/app/pages/students/students.component.html`

```html
<div class="page-container">
  <div class="page-header">
    <div>
      <div class="page-title">Students</div>
      <div class="page-subtitle">Manage registered students in the system</div>
    </div>
    <button class="btn btn-primary" (click)="showForm = !showForm">
      {{ showForm ? '✕ Close' : '+ Add Student' }}
    </button>
  </div>

  <!-- Add Form -->
  <div *ngIf="showForm" class="card form-card">
    <h3 class="form-title">Register New Student</h3>
    <form #f="ngForm" (ngSubmit)="submit(f)" class="student-form">
      <div class="form-grid">
        <div class="form-field">
          <label>CNIE</label>
          <input name="cnie" [(ngModel)]="formData.cnie" required placeholder="e.g. CD2387" pattern="^[A-Z]{1,2}[0-9]{4,8}$" />
        </div>
        <div class="form-field">
          <label>First Name</label>
          <input name="firstName" [(ngModel)]="formData.firstName" required placeholder="First name" />
        </div>
        <div class="form-field">
          <label>Last Name</label>
          <input name="lastName" [(ngModel)]="formData.lastName" required placeholder="Last name" />
        </div>
        <div class="form-field">
          <label>Email</label>
          <input name="email" [(ngModel)]="formData.email" required email type="email" placeholder="email@example.com" />
        </div>
      </div>
      <div class="form-actions">
        <button type="submit" class="btn btn-primary" [disabled]="f.invalid">Register Student</button>
        <button type="button" class="btn btn-outline" (click)="showForm = false">Cancel</button>
      </div>
    </form>
  </div>

  <div *ngIf="error"   class="alert alert-error">⚠ {{ error }}</div>
  <div *ngIf="success" class="alert alert-success">✓ {{ success }}</div>

  <div *ngIf="loading" class="spinner-container">Loading...</div>

  <!-- Table -->
  <div *ngIf="!loading" class="card table-card">
    <div *ngIf="students.length === 0" class="empty-state">
      <div class="empty-icon">◈</div>
      <p>No students registered yet.</p>
    </div>
    <table *ngIf="students.length > 0" class="data-table">
      <thead>
        <tr>
          <th>CNIE</th>
          <th>Full Name</th>
          <th>Email</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let s of students">
          <td><span class="badge badge-accent">{{ s.cnie }}</span></td>
          <td>{{ s.firstName }} {{ s.lastName }}</td>
          <td class="muted">{{ s.email }}</td>
          <td>
            <button class="btn btn-danger btn-sm" (click)="delete(s.id)">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

File: `src/app/pages/students/students.component.scss`

```scss
.page-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 3rem 2.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
}

.form-card { margin-bottom: 1.5rem; }
.form-title { font-family: var(--font-display); margin-bottom: 1.5rem; color: var(--text); }

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
}

.form-actions { display: flex; gap: 1rem; margin-top: 1.5rem; }

.table-card { padding: 0; overflow: hidden; }

.data-table {
  width: 100%;
  border-collapse: collapse;

  th, td { padding: 1rem 1.5rem; text-align: left; }
  th {
    font-size: 0.75rem;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    border-bottom: 1px solid var(--border);
    background: var(--surface-2);
  }
  td { border-bottom: 1px solid rgba(30, 45, 64, 0.5); }
  tr:last-child td { border-bottom: none; }
  tr:hover td { background: rgba(108, 99, 255, 0.04); }
  .muted { color: var(--muted); font-size: 0.9rem; }
}

.btn-sm { padding: 6px 14px; font-size: 0.8rem; }

.empty-state {
  padding: 4rem;
  text-align: center;
  color: var(--muted);
  .empty-icon { font-size: 2.5rem; opacity: 0.3; margin-bottom: 1rem; }
}
```

---

### Task 5.11 — Build Courses page

Generate: `ng g c pages/courses --standalone`

Follow identical structure to the Students page — swap `StudentService` for `CourseService`, `StudentRequest` for `CourseRequest`. The form fields are: `title` (text), `description` (textarea), `credits` (number 1–10).

Add a credits badge to each row: `<span class="badge badge-accent">{{ c.credits }} cr</span>`.

---

### Task 5.12 — Build Enroll page

Generate: `ng g c pages/enroll --standalone`

File: `src/app/pages/enroll/enroll.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../services/course.service';
import { EnrollmentService } from '../../services/enrollment.service';
import { Course } from '../../models/course.model';
import { Enrollment } from '../../models/enrollment.model';

@Component({
  selector: 'app-enroll',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './enroll.component.html',
  styleUrls: ['./enroll.component.scss']
})
export class EnrollComponent implements OnInit {
  courses: Course[] = [];
  selectedCourseId: number | null = null;
  studentCnie = '';
  loading = false;
  error = '';
  success = '';
  result: Enrollment | null = null;

  constructor(
    private courseService: CourseService,
    private enrollmentService: EnrollmentService
  ) {}

  ngOnInit() {
    this.courseService.getAll().subscribe({
      next: (data) => this.courses = data,
      error: () => this.error = 'Failed to load courses.'
    });
  }

  enroll() {
    if (!this.studentCnie.trim() || !this.selectedCourseId) return;
    this.loading = true;
    this.error = '';
    this.success = '';
    this.result = null;

    this.enrollmentService.enroll({
      studentCnie: this.studentCnie.trim(),
      courseId: this.selectedCourseId
    }).subscribe({
      next: (data) => {
        this.result = data;
        this.success = 'Enrollment successful!';
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Enrollment failed.';
        this.loading = false;
      }
    });
  }

  getSelectedCourse(): Course | undefined {
    return this.courses.find(c => c.id === this.selectedCourseId);
  }
}
```

File: `src/app/pages/enroll/enroll.component.html`

```html
<div class="enroll-page">
  <div class="enroll-layout">

    <!-- Left: form -->
    <div class="enroll-form-col">
      <div class="page-header">
        <div class="page-title">Enroll in a Course</div>
        <div class="page-subtitle">Select a course and enter your CNIE to register</div>
      </div>

      <div class="card">
        <div class="form-field" style="margin-bottom:1.25rem">
          <label>Your CNIE</label>
          <input type="text" [(ngModel)]="studentCnie" placeholder="e.g. CD2387" />
        </div>

        <div class="form-field" style="margin-bottom:1.5rem">
          <label>Select Course</label>
          <select [(ngModel)]="selectedCourseId">
            <option [ngValue]="null" disabled>-- Choose a course --</option>
            <option *ngFor="let c of courses" [ngValue]="c.id">
              {{ c.title }} ({{ c.credits }} credits)
            </option>
          </select>
        </div>

        <!-- Selected course preview -->
        <div *ngIf="getSelectedCourse() as course" class="course-preview">
          <div class="cp-title">{{ course.title }}</div>
          <div class="cp-desc">{{ course.description }}</div>
          <span class="badge badge-accent">{{ course.credits }} credits</span>
        </div>

        <button
          class="btn btn-primary enroll-btn"
          [disabled]="!studentCnie || !selectedCourseId || loading"
          (click)="enroll()"
        >
          {{ loading ? 'Enrolling...' : '◉ Confirm Enrollment' }}
        </button>

        <div *ngIf="error"   class="alert alert-error"   style="margin-top:1rem">⚠ {{ error }}</div>
        <div *ngIf="success" class="alert alert-success" style="margin-top:1rem">✓ {{ success }}</div>
      </div>
    </div>

    <!-- Right: result -->
    <div class="enroll-result-col" *ngIf="result">
      <div class="result-card card">
        <div class="result-check">✓</div>
        <h3 class="result-title">Enrolled Successfully</h3>
        <div class="result-detail"><span class="rd-label">CNIE</span> <span>{{ result.studentCnie }}</span></div>
        <div class="result-detail"><span class="rd-label">Course</span> <span>{{ result.courseName }}</span></div>
        <div class="result-detail"><span class="rd-label">Credits</span> <span>{{ result.courseCredits }}</span></div>
        <div class="result-detail">
          <span class="rd-label">Cancellable until</span>
          <span>24h from enrollment</span>
        </div>
        <span [class]="result.deletable ? 'badge badge-success' : 'badge badge-danger'">
          {{ result.deletable ? 'Cancellation open' : 'Window closed' }}
        </span>
      </div>
    </div>

  </div>
</div>
```

File: `src/app/pages/enroll/enroll.component.scss`

```scss
.enroll-page { padding: 3rem 2.5rem; }

.enroll-layout {
  max-width: 960px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  align-items: start;

  @media (max-width: 700px) { grid-template-columns: 1fr; }
}

.enroll-form-col { display: flex; flex-direction: column; gap: 1.5rem; }

.course-preview {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1rem 1.25rem;
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .cp-title { font-family: var(--font-display); font-weight: 700; }
  .cp-desc  { font-size: 0.85rem; color: var(--muted); }
}

.enroll-btn { width: 100%; justify-content: center; padding: 14px; font-size: 0.95rem; }

.result-card {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  animation: slideIn 0.4s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.result-check {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: rgba(0, 212, 170, 0.15);
  border: 2px solid var(--accent-2);
  color: var(--accent-2);
  font-size: 1.75rem;
  display: flex; align-items: center; justify-content: center;
}

.result-title { font-family: var(--font-display); font-size: 1.3rem; }

.result-detail {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
  font-size: 0.9rem;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);

  &:last-of-type { border-bottom: none; }

  .rd-label { color: var(--muted); flex: 1; text-align: left; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.06em; }
}
```

---

### Task 5.13 — Run and verify Angular

```bash
cd enrollment-app
ng serve
```

Open `http://localhost:4200`. Verify:

- Navbar highlights active route
- Dashboard stats load (student and course counts)
- Dashboard CNIE search returns enrollments with cancel buttons
- Students page: table loads, add form creates new student
- Courses page: table loads, add form creates new course
- Enroll page: dropdown loads all courses, enrollment submits and shows result card

---

## PHASE 6 — Final Integration Verification

### Task 6.1 — Full end-to-end test

Run all services simultaneously:

```bash
# Terminal 1
cd student-service  && mvn spring-boot:run

# Terminal 2
cd course-service   && mvn spring-boot:run

# Terminal 3
cd enrollment-service && mvn spring-boot:run

# Terminal 4
cd api-gateway      && mvn spring-boot:run

# Terminal 5
cd frontend/enrollment-app && ng serve
```

### Task 6.2 — Test scenario

1. Go to **Students** → add student `CD2387`
2. Go to **Courses** → add `Spring Framework` (4 credits) and `Data Mining` (3 credits)
3. Go to **Enroll** → enroll `CD2387` in `Spring Framework`
4. Enroll `CD2387` in `Data Mining`
5. Try enrolling a 4th student in same course → expect "course is full" error
6. Go to **Dashboard** → search `CD2387` → see both enrollments with `Cancellable` badge
7. Cancel one → it disappears from the list
8. Wait 24h (or set `enrollmentDate` to 25h ago in DB) → Delete button disabled

### Task 6.3 — Verify all Gateway routes

```bash
curl http://localhost:8080/api/students
curl http://localhost:8080/api/courses
curl http://localhost:8080/api/enrollments/student/CD2387
```

All must return valid JSON through port 8080 only.

---

## SUMMARY — Project Structure

```
student-enrollment-system/
├── student-service/          → Port 8081 · PostgreSQL: student_db
│   └── entity/ dto/ mapper/ repository/ service/ controller/ exception/ config/
├── course-service/           → Port 8082 · PostgreSQL: course_db
│   └── entity/ dto/ mapper/ repository/ service/ controller/ exception/ config/
├── enrollment-service/       → Port 8083 · PostgreSQL: enrollment_db
│   └── entity/ dto/ client/ repository/ service/ controller/ exception/ config/
├── api-gateway/              → Port 8080 · Routes all requests
└── frontend/enrollment-app/  → Port 4200 · Angular 18 SPA
    └── models/ services/ components/ pages/
```

---

## CHECKLIST

- [ ] 3 PostgreSQL databases created manually before first boot (`student_db`, `course_db`, `enrollment_db`)
- [ ] All services start without errors
- [ ] API Gateway routes all endpoints correctly
- [ ] Student CRUD works end-to-end
- [ ] Course CRUD works end-to-end
- [ ] Enrollment validates: student exists, course exists, max 3 cap, no duplicate
- [ ] `deletable` flag reflects 24h rule correctly
- [ ] Angular navbar routes between all 4 pages
- [ ] Dashboard stats show real counts
- [ ] Enroll page dropdown loads courses dynamically
- [ ] All error messages surface cleanly in the UI
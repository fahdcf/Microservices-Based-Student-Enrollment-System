# Microservices-Based Student Enrollment System

Welcome to the definitive guide for the **Student Enrollment System**. This project is a complete, full-stack, distributed microservices application built with **Spring Boot 3** and **Angular 18**. 

I designed this project to handle university-style course registrations while strictly enforcing business rules (like course capacity limits and cancellation validation). By adopting a microservice architecture, every domain (Students, Courses, and Enrollments) remains isolated, independently scalable, and maintains its own database.

---

## 🏗️ Architecture & Technologies

### Backend Stack
*   **Java 17 & Spring Boot 3.4.1**: Core framework for all services.
*   **Spring Cloud Netflix Eureka**: Service Registry and Discovery.
*   **Spring Cloud Gateway**: Single entry point / Edge server routing traffic.
*   **Spring Data JPA & Hibernate**: ORM mapping and data persistence.
*   **PostgreSQL 17**: Relational databases for the microservices.
*   **Spring WebFlux (WebClient)**: For reactive/synchronous inter-service communication.

### Frontend Stack
*   **Angular 18**: Component-based Single Page Application (SPA).
*   **SCSS**: Custom design system featuring CSS variables, flexbox, and CSS Grids.

---

## 🗺️ System Blueprint

The environment consists of **5 Java Services** and **1 Angular Application**.

| Component | Port | Database | Purpose |
| :--- | :--- | :--- | :--- |
| **Eureka Server** | `8761` | *None* | Service Registry. All backend microservices register themselves here on startup. |
| **API Gateway** | `8080` | *None* | The edge layer. The frontend only talks to `8080`. Gateway routes `/api/students/**` to 8081, `/api/courses/**` to 8082, etc. |
| **Student Service** | `8081` | `student_db` | Manages `Student` entities (ID, Name, CNIE). Provides basic CRUD APIs. |
| **Course Service** | `8082` | `course_db` | Manages `Course` entities (ID, Title, Description, Credits). Provides basic CRUD APIs. |
| **Enrollment Service** | `8083` | `enrollment_db` | The core business logic orchestrator orchestrating rules and relationships. |
| **Frontend App** | `4200` | *None* | The user interface. |

---

## 🧠 Deep Dive: How the Code Works

### 1. Database Isolation (Database-per-service pattern)
I implemented a strict database-per-service architecture. `student-service`, `course-service`, and `enrollment-service` never talk to each other's databases directly. 
*   **Configuration**: Inside each service's `src/main/resources/application.properties`, you will see `spring.datasource.url=jdbc:postgresql://localhost:5432/<db_name>`. 
*   **Auto-DDL**: I use `spring.jpa.hibernate.ddl-auto=update` so the tables auto-generate upon startup.

### 2. Inter-Service Communication
The `enrollment-service` is the brains of the operation. Before a student can enroll in a course, the service must verify that:
1. The student exists.
2. The course exists.

To achieve this, I built **WebClient API Clients** inside the `enrollment-service`:
*   `StudentClient.java`: Makes an HTTP GET request to `http://student-service/api/students/cnie/{cnie}`.
*   `CourseClient.java`: Makes an HTTP GET request to `http://course-service/api/courses/{id}`.
*Notice it uses the logical names (`student-service`) rather than physical localhost IPs because Eureka handles the discovery!*

### 3. Business Logic Execution
Open `EnrollmentService.java` inside `enrollment-service` to see the core rules:
*   **Rule 1: Course Cap (Max 3 Students)**
    When an enrollment is requested, the code queries `enrollmentRepository.countByCourseId(courseId)`. If this is `>= 3`, a custom `CourseFullException` is thrown and caught by the GlobalExceptionHandler to return a clean 400 Bad Request.
*   **Rule 2: The 24-Hour Cancellation Window**
    When a student attempts to un-enroll, I calculate the time elapsed using Java's `LocalDateTime`. If `ChronoUnit.HOURS.between(enrollment.getEnrollmentDate(), now) > 24`, the system throws an exception blocking the drop.

### 4. The Frontend (`frontend/enrollment-app`)
The Angular application provides a seamless dashboard.
*   **Models**: Under `src/app/models/`, I mapped TypeScript interfaces directly to the Java DTOs (`StudentResponse`, `CourseResponse`, `EnrollmentResponse`).
*   **Services**: The `src/app/services/` logic uses Angular's `HttpClient` to speak *only* to the `http://localhost:8080/api/...` endpoint (API Gateway). The UI has no idea the other microservices exist.
*   **Pages**:
    *   `students/` and `courses/`: Maintain basic data entry.
    *   `enroll/`: This component aggregates data. It fetches all courses, lets you input a CNIE, pushes to the enrollment API, and has customized dynamic CSS (utilizing `flexbox`) to perfectly center the success confirmation card.

---

## 📂 Project Structure

```text
student-enrollment-system
â”œâ”€â”€ api-gateway/            # Spring Cloud Gateway (Routes backend traffic)
â”œâ”€â”€ course-service/         # Course bounded context (Entity, Repo, Service, Controller)
â”œâ”€â”€ enrollment-service/     # Enrollment domain + WebClients + Business Logic
â”œâ”€â”€ eureka-server/          # Service Registry
â”œâ”€â”€ frontend/               
â”‚   â””â”€â”€ enrollment-app/     # Angular 18 Dashboard application
â”œâ”€â”€ student-service/        # Student bounded context
â””â”€â”€ README.md               # You are here!
```

---

## 🚀 How to Run the Project Locally

Because microservices are RAM intensive, starting all 5 Java services normally might crash your OS with an `insufficient memory` (malloc) error, generating a `hs_err_pid*.log` file. 

To run the whole suite perfectly:

### Step 1: Database Setup
Ensure PostgreSQL natively running on port `5432` with password `fahd`.
Run these commands in `psql` to prepare the databases:
```sql
CREATE DATABASE student_db;
CREATE DATABASE course_db;
CREATE DATABASE enrollment_db;
```

### Step 2: Start Eureka & API Gateway
Open terminals in their respective directories and cap their memory usage (this is vital):
```bash
cd eureka-server
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"

cd ../api-gateway
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"
```

### Step 3: Start the Backend Domains
Start the three main services the exact same way in their own terminals:
```bash
cd ../student-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"

cd ../course-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"

cd ../enrollment-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"
```

### Step 4: Start Angular Frontend
Open one last terminal for the frontend:
```bash
cd frontend/enrollment-app
npm install
ng serve
```

Navigate to `http://localhost:4200` to view the finalized application!

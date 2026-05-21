# CLAUDE.md — Autonomous Build Instructions

You are building a **Microservices-Based Student Enrollment System** end-to-end.
Your source of truth for every piece of code, configuration, and structure is **`tasks.md`**.

---

## Core Directive

Work through `tasks.md` **sequentially, one task at a time**.
Do not skip tasks. Do not batch tasks. Do not move to the next task until the current one is verified and committed to GitHub.

---

## Mandatory Workflow Per Task

For **every single task** in `tasks.md`, execute this exact loop:

```
1. READ   → Read the task fully before writing any code
2. BUILD  → Implement exactly what the task specifies, nothing more
3. VERIFY → Run the verification steps defined below
4. COMMIT → Stage, commit, and push to GitHub
5. NEXT   → Only then move to the next task
```

Never collapse multiple tasks into one commit. One task = one commit.

---

## Step 1 — READ

Before touching any file:
- Read the full task description in `tasks.md`
- Understand what files need to be created or modified
- Identify what the verification should look like
- If the task references a previous task's output (e.g. "copy CorsConfig from student-service"), locate that file first

---

## Step 2 — BUILD

- Create or edit files exactly as specified in the task
- Use the exact package names, class names, field names, and annotations shown
- Do not add extra logic, extra endpoints, or extra dependencies not mentioned in the task
- If the task says to copy a file from another service, do so and update only the package declaration

---

## Step 3 — VERIFY

After building, run the appropriate verification for the task type:

### For `application.properties` / `application.yml` tasks:
```bash
# Confirm file exists and has no syntax errors
cat src/main/resources/application.properties
```

### For Java class tasks (entity, repository, DTO, mapper, service, controller):
```bash
# Compile the service to catch any errors
mvn compile -q
```
If `mvn compile` passes with no errors, verification is done.

### For full service startup tasks (e.g. Task 1.12, 2.10, 3.11):
```bash
# Start the service in background, wait 8 seconds, then test the endpoint
mvn spring-boot:run &
sleep 8
curl -s -o /dev/null -w "%{http_code}" http://localhost:<PORT>/api/<resource>
# Expected: 200
kill %1
```

### For API Gateway task (Task 4.4):
Start all 3 backend services, then start the gateway and verify routing:
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/students
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/courses
# Expected: 200 for both
```

### For Angular scaffold tasks (Task 5.1 — 5.2):
```bash
cd frontend/enrollment-app
ng build --configuration development 2>&1 | tail -5
# Expected: "Build at: ... - Time: ...ms"
```

### For Angular component/service/page tasks (Task 5.3 — 5.12):
```bash
cd frontend/enrollment-app
ng build --configuration development 2>&1 | tail -5
# Expected: no errors, successful build
```

### For Angular final run (Task 5.13):
```bash
cd frontend/enrollment-app
ng serve &
sleep 10
curl -s -o /dev/null -w "%{http_code}" http://localhost:4200
kill %1
# Expected: 200
```

### For Phase 6 integration tasks:
Run the full scenario described in Task 6.2 manually using curl, confirming each step returns the expected HTTP status and JSON shape.

---

## Step 4 — COMMIT

After verification passes, commit and push:

```bash
git add .
git commit -m "<type>(<scope>): <description>"
git push origin main
```

### Commit message format

Use this convention strictly:

| Type | When to use |
|------|-------------|
| `feat` | New file or feature added |
| `config` | Configuration file created or modified |
| `fix` | Bug or error corrected |
| `test` | Verification or test step |
| `chore` | Scaffolding, folder creation, README |

**Examples by task:**

```
chore(root): initialize project structure and README
config(student-service): add PostgreSQL application.properties
feat(student-service): add Student entity with validation
feat(student-service): add StudentRepository
feat(student-service): add StudentRequest and StudentResponse DTOs
feat(student-service): add StudentMapper
feat(student-service): add StudentService with business logic
feat(student-service): add StudentController REST endpoints
feat(student-service): add GlobalExceptionHandler
feat(student-service): add CorsConfig
feat(course-service): add Course entity
feat(enrollment-service): add Enrollment entity with unique constraint
feat(enrollment-service): add WebClient configuration
feat(enrollment-service): add StudentClient and CourseClient
feat(enrollment-service): add EnrollmentService with 3-student cap and 24h rule
feat(enrollment-service): add EnrollmentController
config(api-gateway): add Spring Cloud Gateway routes and CORS
chore(frontend): scaffold Angular 18 project
feat(frontend): add global SCSS design system
feat(frontend): add API models and services
feat(frontend): add routing and app shell
feat(frontend): add NavbarComponent
feat(frontend): add DashboardComponent with CNIE lookup
feat(frontend): add StudentsComponent with CRUD table
feat(frontend): add CoursesComponent with CRUD table
feat(frontend): add EnrollComponent with course preview
```

---

## Step 5 — NEXT

Only after the commit is pushed successfully, print:

```
✓ Task X.Y complete — committed as: <commit message>
→ Moving to Task X.Z
```

Then immediately begin Task X.Z.

---

## GitHub Setup (run once before Task 0.1)

```bash
git init
git remote add origin <REPO_URL>
git checkout -b main
```

Replace `<REPO_URL>` with the actual repository URL before starting.
If the repo already exists and is cloned, skip `git init` and `git remote add`.

---

## Rules

1. **Never skip a task** — every task in `tasks.md` must be executed in order
2. **Never batch commits** — one task = one commit, always
3. **Never proceed on a failed verify** — if `mvn compile` or `ng build` fails, fix the error before committing
4. **Never modify tasks.md** — it is read-only, treat it as the spec
5. **Never hallucinate dependencies** — only add what `tasks.md` explicitly lists
6. **Always kill background processes** after port tests so ports stay free
7. **Always use the exact class/file names** from `tasks.md` — no renaming
8. **If a service port is already in use**, kill the process: `lsof -ti:<PORT> | xargs kill -9`

---

## Error Recovery

If a build or compile fails:

1. Read the full error message
2. Identify which file caused it
3. Fix only that file
4. Re-run `mvn compile` or `ng build`
5. Do not move to the next task until it passes
6. Commit the fix with message: `fix(<scope>): <what was wrong>`

---

## Phase Boundaries

At the end of each Phase (0 through 6), after the last task's commit, push a phase summary tag:

```bash
git tag phase-<N>-complete
git push origin phase-<N>-complete
```

Example after Phase 1: `git tag phase-1-complete && git push origin phase-1-complete`

---

## Quick Reference — Ports

| Service            | Port |
|--------------------|------|
| student-service    | 8081 |
| course-service     | 8082 |
| enrollment-service | 8083 |
| api-gateway        | 8080 |
| Angular frontend   | 4200 |

## Quick Reference — Databases (PostgreSQL)

| Service            | Database      |
|--------------------|---------------|
| student-service    | student_db    |
| course-service     | course_db     |
| enrollment-service | enrollment_db |

Create all three before running any service:
```bash
psql -U postgres -c "CREATE DATABASE student_db;"
psql -U postgres -c "CREATE DATABASE course_db;"
psql -U postgres -c "CREATE DATABASE enrollment_db;"
```

---

## Start Here

```
1. Set up the GitHub repo and run git init
2. Open tasks.md
3. Begin Task 0.1
4. Follow the READ → BUILD → VERIFY → COMMIT → NEXT loop until all tasks are done
```
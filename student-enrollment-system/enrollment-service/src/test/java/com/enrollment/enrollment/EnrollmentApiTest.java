package com.enrollment.enrollment;

import com.enrollment.enrollment.client.*;
import com.enrollment.enrollment.dto.*;
import com.enrollment.enrollment.service.EnrollmentService;
import com.enrollment.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
 "security.jwt.secret=test-only-signing-key-32-bytes-long-never-deploy", "ADMIN_USERNAME=test-admin", "ADMIN_PASSWORD=test-admin-password",
 "spring.datasource.username=${TEST_DB_USERNAME:enrollment_test}", "spring.datasource.password=${TEST_DB_PASSWORD:unused}",
 "spring.datasource.url=${TEST_ENROLLMENT_DB_URL:jdbc:postgresql://127.0.0.1:55432/enrollment_test}"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="TEST_DATABASES", matches="true")
class EnrollmentApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EnrollmentService service;
    @Autowired EnrollmentRepository repository;
    @Autowired JwtDecoder decoder;
    @MockitoBean StudentClient students;
    @MockitoBean CourseClient courses;
    @MockitoBean Clock clock;
    Instant now=Instant.parse("2026-01-01T12:00:00Z");
    String admin="Bearer "+token("ADMIN","","");
    String owner="Bearer "+token("STUDENT","1","AB1234");

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM enrollments"); jdbc.update("DELETE FROM enrollment_references");
        when(clock.instant()).thenReturn(now);
        when(students.getStudentByCnie(anyString())).thenAnswer(a -> {
            String cnie=a.getArgument(0);
            long id=cnie.equals("AB1234")?1:Long.parseLong(cnie.substring(2));
            return new StudentDTO(id,cnie,"Test","User","test@example.test");
        });
        when(students.getStudentById(anyLong())).thenAnswer(a ->
            new StudentDTO(a.getArgument(0),"AB1234","Test","User","test@example.test"));
        when(courses.getCourseById(anyLong())).thenAnswer(a ->
            new CourseDTO(a.getArgument(0),"Java","Introduction",4));
        authenticate();
    }
    @AfterEach void clearAuth() { org.springframework.security.test.context.TestSecurityContextHolder.clearContext(); }
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            decoder.decode(admin.substring(7)),List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
    long enroll(String cnie) { authenticate(); return service.enrollStudent(new EnrollmentRequest(cnie,1L)).getEnrollmentId(); }

    @Test void successfulEnrollmentDuplicateAndCapacity() throws Exception {
        mvc.perform(post("/api/enrollments").header("Authorization",owner).contentType("application/json")
            .content("{\"studentCnie\":\"AB1234\",\"courseId\":1}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.deletable").value(true));
        mvc.perform(get("/api/enrollments/student/AB1234").header("Authorization",owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        assertThatThrownBy(() -> enroll("AB1234")).isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
        enroll("AB1235"); enroll("AB1236");
        assertThatThrownBy(() -> enroll("AB1237")).hasMessageContaining("Course is full");
        assertThat(repository.countByCourseId(1L)).isEqualTo(3);
    }

    @Test void missingResourcesAndUnavailableDependenciesAreDifferent() throws Exception {
        when(students.getStudentByCnie("AB9999")).thenThrow(new ResponseStatusException(NOT_FOUND,"Student not found"));
        mvc.perform(post("/api/enrollments").header("Authorization",admin).contentType("application/json")
            .content("{\"studentCnie\":\"AB9999\",\"courseId\":1}")).andExpect(status().isNotFound());
        when(courses.getCourseById(2L)).thenThrow(new ResponseStatusException(NOT_FOUND,"Course not found"));
        mvc.perform(post("/api/enrollments").header("Authorization",admin).contentType("application/json")
            .content("{\"studentCnie\":\"AB1234\",\"courseId\":2}")).andExpect(status().isNotFound());
        when(courses.getCourseById(1L)).thenThrow(new ResponseStatusException(SERVICE_UNAVAILABLE,"Course unavailable"));
        mvc.perform(post("/api/enrollments").header("Authorization",admin).contentType("application/json")
            .content("{\"studentCnie\":\"AB1234\",\"courseId\":1}")).andExpect(status().isServiceUnavailable());
        assertThat(repository.count()).isZero();
    }

    @Test void concurrentLastSeatUsesDatabaseLock() throws Exception {
        enroll("AB1234"); enroll("AB1235");
        var barrier=new CyclicBarrier(4);
        doAnswer(a -> {
            String cnie=a.getArgument(0);
            barrier.await(10,TimeUnit.SECONDS);
            return new StudentDTO(Long.parseLong(cnie.substring(2)),cnie,"Test","User","t@example.test");
        }).when(students).getStudentByCnie(startsWith("CD"));
        var pool=Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for (int i=2000;i<2004;i++) {
                String cnie="CD"+i;
                results.add(pool.submit(() -> {
                    authenticate();
                    try { service.enrollStudent(new EnrollmentRequest(cnie,1L)); return true; }
                    catch (ResponseStatusException ex) { assertThat(ex.getStatusCode().value()).isEqualTo(409); return false; }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            int successes=0;
            for (var result:results) if(result.get(20,TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
            assertThat(repository.countByCourseId(1L)).isEqualTo(3);
        } finally { pool.shutdownNow(); }
    }

    @Test void cancellationWindowIsStrictAndDoubleCancellationIsMissing() throws Exception {
        long id=enroll("AB1234");
        jdbc.update("UPDATE enrollments SET enrollment_date=? WHERE id=?",java.sql.Timestamp.from(now.minus(Duration.ofHours(24)).plusMillis(1)),id);
        mvc.perform(delete("/api/enrollments/"+id).header("Authorization",owner)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/enrollments/"+id).header("Authorization",owner)).andExpect(status().isNotFound());
        id=enroll("AB1234");
        jdbc.update("UPDATE enrollments SET enrollment_date=? WHERE id=?",java.sql.Timestamp.from(now.minus(Duration.ofHours(24))),id);
        mvc.perform(delete("/api/enrollments/"+id).header("Authorization",owner)).andExpect(status().isConflict());
        jdbc.update("UPDATE enrollments SET enrollment_date=? WHERE id=?",java.sql.Timestamp.from(now.minus(Duration.ofHours(25))),id);
        mvc.perform(delete("/api/enrollments/"+id).header("Authorization",owner)).andExpect(status().isConflict());
    }

    @Test void unauthorizedAndOtherStudentsCannotAccessOrCancel() throws Exception {
        long id=enroll("AB1234");
        String other="Bearer "+token("STUDENT","2","CD1234");
        clearAuth();
        mvc.perform(get("/api/enrollments/student/AB1234")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/enrollments/student/AB1234").header("Authorization",other)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/enrollments/"+id).header("Authorization",other)).andExpect(status().isForbidden());
        mvc.perform(post("/api/enrollments").header("Authorization",other).contentType("application/json")
            .content("{\"studentCnie\":\"AB1234\",\"courseId\":1}")).andExpect(status().isForbidden());
        mvc.perform(post("/internal/references/courses/1/close").header("Authorization",owner)).andExpect(status().isForbidden());
    }

    @Test void deletionBlocksActiveReferencesAndClosesEmptyOnesIdempotently() throws Exception {
        long id=enroll("AB1234");
        assertThatThrownBy(() -> service.closeStudent(1L)).hasMessageContaining("409");
        assertThatThrownBy(() -> service.closeCourse(1L)).hasMessageContaining("409");
        service.cancelEnrollment(id);
        service.closeStudent(1L); service.closeStudent(1L);
        assertThatThrownBy(() -> enroll("AB1234")).hasMessageContaining("being deleted");
        service.closeCourse(1L);
        assertThatThrownBy(() -> enroll("AB1235")).hasMessageContaining("being deleted");
    }

    @Test void deletionAndEnrollmentOverlapWithoutDanglingReference() throws Exception {
        var entered=new CountDownLatch(1); var release=new CountDownLatch(1);
        when(courses.getCourseById(1L)).thenAnswer(a -> {
            entered.countDown(); assertThat(release.await(10,TimeUnit.SECONDS)).isTrue();
            return new CourseDTO(1L,"Java","Intro",4);
        });
        var pool=Executors.newFixedThreadPool(2);
        try {
            var enrollment=pool.submit(() -> { authenticate(); try { return service.enrollStudent(new EnrollmentRequest("AB1234",1L)); } finally { clearAuth(); } });
            assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
            var deletion=pool.submit(() -> { authenticate(); try { service.closeCourse(1L); return 204; } catch(ResponseStatusException ex) { return ex.getStatusCode().value(); } finally { clearAuth(); } });
            release.countDown();
            assertThat(enrollment.get(15,TimeUnit.SECONDS)).isNotNull();
            assertThat(deletion.get(15,TimeUnit.SECONDS)).isEqualTo(409);
            assertThat(repository.count()).isEqualTo(1);
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test void databaseRejectsDuplicateEvenWithoutServiceCheck() {
        enroll("AB1234");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO enrollments(student_id,course_id,enrollment_date) VALUES (1,1,now())"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    static String token(String role, String id, String cnie) {
        var key = new javax.crypto.spec.SecretKeySpec("test-only-signing-key-32-bytes-long-never-deploy".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        var now = java.time.Instant.now();
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("student-enrollment").subject("test-user")
            .audience(java.util.List.of("enrollment-api")).issuedAt(now).expiresAt(now.plusSeconds(900))
            .claim("roles", java.util.List.of(role)).claim("student_id", id).claim("cnie", cnie).build();
        return encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
            org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

}

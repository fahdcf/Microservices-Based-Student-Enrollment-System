package com.enrollment.course;

import com.enrollment.course.client.EnrollmentClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties={"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
 "security.jwt.secret=test-only-signing-key-32-bytes-long-never-deploy", "ADMIN_USERNAME=test-admin", "ADMIN_PASSWORD=test-admin-password",
 "spring.datasource.username=${TEST_DB_USERNAME:enrollment_test}", "spring.datasource.password=${TEST_DB_PASSWORD:unused}",
 "spring.datasource.url=${TEST_COURSE_DB_URL:jdbc:postgresql://127.0.0.1:55432/course_test}"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="TEST_DATABASES", matches="true")
class CourseApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean EnrollmentClient enrollments;
    String admin = "Bearer " + token("ADMIN", "", "");
    String student = "Bearer " + token("STUDENT", "999", "ZZ9999");
    String url = "/api/courses";
    String body = "{\"title\":\"Java\",\"description\":\"Introduction\",\"credits\":4}";

    @BeforeEach void clean() { jdbc.update("DELETE FROM courses"); }

    long create() throws Exception {
        var response = mvc.perform(post(url).header("Authorization",admin).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test void crudAndMissingResource() throws Exception {
        long id = create();
        mvc.perform(get(url+"/"+id).header("Authorization",admin)).andExpect(status().isOk());
        mvc.perform(put(url+"/"+id).header("Authorization",admin).contentType("application/json").content(body.replace("Java", "Updated Java")))
            .andExpect(status().isOk());
        mvc.perform(get(url+"/"+id).header("Authorization",admin))
            .andExpect(jsonPath("$.title").value("Updated Java"));
        mvc.perform(delete(url+"/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
        verify(enrollments).closeCourseForEnrollment(id);
        mvc.perform(get(url+"/"+id).header("Authorization",admin)).andExpect(status().isNotFound());
    }

    @Test void invalidAndDuplicateRequests() throws Exception {
        mvc.perform(post(url).header("Authorization",admin).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(url+"/-1").header("Authorization",admin)).andExpect(status().isBadRequest());
        create();
        mvc.perform(post(url).header("Authorization",admin).contentType("application/json").content(body))
            .andExpect(status().isConflict());
    }

    @Test void deletionRejectsReferencesAndDependencyFailure() throws Exception {
        long id = create();
        doThrow(new ResponseStatusException(CONFLICT,"Active enrollments")).when(enrollments).closeCourseForEnrollment(id);
        mvc.perform(delete(url+"/"+id).header("Authorization",admin)).andExpect(status().isConflict());
        mvc.perform(get(url+"/"+id).header("Authorization",admin)).andExpect(status().isOk());
        doThrow(new ResponseStatusException(SERVICE_UNAVAILABLE,"Unavailable")).when(enrollments).closeCourseForEnrollment(id);
        mvc.perform(delete(url+"/"+id).header("Authorization",admin)).andExpect(status().isServiceUnavailable());
    }

    @Test void directServiceSecurityValidatesTokensAndRoles() throws Exception {
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header("Authorization","Bearer broken")).andExpect(status().isUnauthorized());
        mvc.perform(post(url).header("Authorization",student).contentType("application/json").content(body))
            .andExpect(status().isForbidden());
        mvc.perform(get(url).header("Authorization",admin)).andExpect(status().isOk());
        mvc.perform(get(url).header("Authorization",student)).andExpect(status().isOk());
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

package com.enrollment.student;

import com.enrollment.student.client.EnrollmentClient;
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
 "spring.datasource.url=${TEST_STUDENT_DB_URL:jdbc:postgresql://127.0.0.1:55432/student_test}"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="TEST_DATABASES", matches="true")
class StudentApiTest {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean EnrollmentClient enrollments;
    String admin = "Bearer " + token("ADMIN", "", "");
    String student = "Bearer " + token("STUDENT", "999", "ZZ9999");
    String url = "/api/students";
    String body = "{\"cnie\":\"AB1234\",\"firstName\":\"Test\",\"lastName\":\"Student\",\"email\":\"test@example.test\"}";

    @BeforeEach void clean() { jdbc.update("DELETE FROM students"); }

    long create() throws Exception {
        var response = mvc.perform(post(url).header("Authorization",admin).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test void crudAndMissingResource() throws Exception {
        long id = create();
        mvc.perform(get(url+"/"+id).header("Authorization",admin)).andExpect(status().isOk());
        mvc.perform(put(url+"/"+id).header("Authorization",admin).contentType("application/json").content(body.replace("Test", "Updated")))
            .andExpect(status().isOk());
        mvc.perform(get(url+"/"+id).header("Authorization",admin))
            .andExpect(jsonPath("$.firstName").value("Updated"));
        mvc.perform(delete(url+"/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
        verify(enrollments).closeStudentForEnrollment(id);
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
        doThrow(new ResponseStatusException(CONFLICT,"Active enrollments")).when(enrollments).closeStudentForEnrollment(id);
        mvc.perform(delete(url+"/"+id).header("Authorization",admin)).andExpect(status().isConflict());
        mvc.perform(get(url+"/"+id).header("Authorization",admin)).andExpect(status().isOk());
        doThrow(new ResponseStatusException(SERVICE_UNAVAILABLE,"Unavailable")).when(enrollments).closeStudentForEnrollment(id);
        mvc.perform(delete(url+"/"+id).header("Authorization",admin)).andExpect(status().isServiceUnavailable());
    }

    @Test void directServiceSecurityValidatesTokensAndRoles() throws Exception {
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header("Authorization","Bearer broken")).andExpect(status().isUnauthorized());
        mvc.perform(post(url).header("Authorization",student).contentType("application/json").content(body))
            .andExpect(status().isForbidden());
        mvc.perform(get(url).header("Authorization",admin)).andExpect(status().isOk());
        long id = create();
        mvc.perform(get(url+"/"+id).header("Authorization",student)).andExpect(status().isForbidden());
        mvc.perform(get(url+"/cnie/AB1234").header("Authorization",student)).andExpect(status().isForbidden());
        mvc.perform(get(url+"/"+id).header("Authorization","Bearer "+token("STUDENT",Long.toString(id),"AB1234")))
            .andExpect(status().isOk());
    }

    @Test void loginAndAdminProvisionedAccounts() throws Exception {
        long id = create();
        String username = "student-" + id;
        mvc.perform(post("/api/auth/accounts").header("Authorization",student).contentType("application/json")
            .content("{\"username\":\"bad\",\"password\":\"long-password\",\"studentId\":"+id+"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/accounts").header("Authorization",admin).contentType("application/json")
            .content("{\"username\":\""+username+"\",\"password\":\"long-password\",\"studentId\":"+id+",\"role\":\"ADMIN\"}"))
            .andExpect(status().isCreated());
        var login=mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\""+username+"\",\"password\":\"long-password\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT")).andReturn();
        String jwt=json.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get(url+"/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isOk());
        mvc.perform(get(url).header("Authorization","Bearer "+jwt)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\""+username+"\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
    }



    @Test void rejectsExpiredWrongIssuerAndWrongAudienceTokens() {
        var key = new javax.crypto.spec.SecretKeySpec("test-only-signing-key-32-bytes-long-never-deploy".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        var now = java.time.Instant.now();
        for (int invalid=0;invalid<4;invalid++) {
            var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer(invalid==1 ? "wrong-issuer" : "student-enrollment").subject("test")
                .audience(java.util.List.of(invalid==2 ? "wrong-api" : "enrollment-api"))
                .issuedAt(now.minusSeconds(7200));
            if (invalid!=3) claims.expiresAt(invalid==0 ? now.minusSeconds(3600) : now.plusSeconds(900));
            String signed = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> decoder.decode(signed))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> decoder.decode(token("ADMIN","","")+"changed"))
            .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
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

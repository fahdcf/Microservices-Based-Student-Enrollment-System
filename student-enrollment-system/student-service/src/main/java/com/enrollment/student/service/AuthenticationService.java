package com.enrollment.student.service;

import com.enrollment.student.entity.UserAccount;
import com.enrollment.student.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final long TOKEN_LIFETIME_SECONDS = 900;
    private static final String UNKNOWN_USER_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private final UserAccountRepository accounts;
    private final StudentRepository students;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    @Value("${security.jwt.issuer}") private String issuer;

    public record LoginResponse(String accessToken, long expiresIn, String role, String cnie, Long studentId) {}

    public LoginResponse login(String username, String password) {
        UserAccount account = accounts.findByUsername(username).orElse(null);
        // Use a real BCrypt comparison even for unknown users.
        String hash = account == null ? UNKNOWN_USER_HASH : account.getPasswordHash();
        if (!passwords.matches(password, hash) || account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid username or password");
        }
        String cnie = null;
        if (account.getStudentId() != null) {
            cnie = students.findById(account.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Account is no longer active")).getCnie();
        }
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer(issuer).subject(account.getUsername())
            .audience(List.of("enrollment-api")).issuedAt(now).expiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS))
            .claim("roles", List.of(account.getRole()));
        if (cnie != null) claims.claim("cnie", cnie).claim("student_id", account.getStudentId().toString());
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new LoginResponse(token, TOKEN_LIFETIME_SECONDS, account.getRole(), cnie, account.getStudentId());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void createStudentAccount(String username, String password, Long studentId) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        if (!students.existsById(studentId)) throw new ResponseStatusException(NOT_FOUND, "Student not found");
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwords.encode(password));
        account.setRole("STUDENT");
        account.setStudentId(studentId);
        accounts.saveAndFlush(account);
    }
}

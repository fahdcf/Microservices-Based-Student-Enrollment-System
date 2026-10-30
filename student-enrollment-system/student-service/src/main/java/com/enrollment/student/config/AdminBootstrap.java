package com.enrollment.student.config;
import com.enrollment.student.entity.UserAccount;
import com.enrollment.student.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    @Value("${ADMIN_USERNAME:}") private String username;
    @Value("${ADMIN_PASSWORD:}") private String password;
    @Override public void run(ApplicationArguments args) {
        if (accounts.existsByRole("ADMIN")) return;
        if (username.isBlank() || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Set ADMIN_USERNAME and ADMIN_PASSWORD (12-72 characters) to bootstrap the first administrator");
        }
        UserAccount admin = new UserAccount();
        admin.setUsername(username);
        admin.setPasswordHash(passwords.encode(password));
        admin.setRole("ADMIN");
        accounts.save(admin);
    }
}

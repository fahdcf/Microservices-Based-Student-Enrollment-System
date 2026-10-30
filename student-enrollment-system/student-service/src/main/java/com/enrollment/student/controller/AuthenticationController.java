package com.enrollment.student.controller;
import com.enrollment.student.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService service;
    public record LoginRequest(@NotBlank @Size(max=100) String username, @NotBlank @Size(max=72) String password) {}
    public record AccountRequest(@NotBlank @Size(max=100) String username,
        @NotBlank @Size(min=12,max=72) String password, @NotNull @Positive Long studentId) {}
    @PostMapping("/login")
    AuthenticationService.LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.username(), request.password());
    }
    @PostMapping("/accounts")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    void account(@Valid @RequestBody AccountRequest request) {
        service.createStudentAccount(request.username(), request.password(), request.studentId());
    }
}

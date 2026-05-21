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

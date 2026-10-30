package com.enrollment.enrollment.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EnrollmentRequest {
    @NotBlank @Pattern(regexp = "^[A-Z]{1,2}[0-9]{4,8}$")
    private String studentCnie;
    @NotNull @Positive
    private Long courseId;
}

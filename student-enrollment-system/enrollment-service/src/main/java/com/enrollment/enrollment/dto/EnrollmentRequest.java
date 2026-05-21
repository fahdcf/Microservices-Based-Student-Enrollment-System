package com.enrollment.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EnrollmentRequest {

    @NotBlank(message = "Student CNIE is required")
    private String studentCnie;

    @NotNull(message = "Course ID is required")
    private Long courseId;
}

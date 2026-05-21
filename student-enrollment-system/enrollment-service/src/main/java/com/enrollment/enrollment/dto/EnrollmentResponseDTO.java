package com.enrollment.enrollment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentResponseDTO {
    private Long enrollmentId;
    private String studentCnie;
    private String courseName;
    private int courseCredits;
    private LocalDateTime enrollmentDate;
    private boolean deletable;
}

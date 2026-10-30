package com.enrollment.enrollment.dto;

import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentResponseDTO {
    private Long enrollmentId;
    private String studentCnie;
    private String courseName;
    private int courseCredits;
    private Instant enrollmentDate;
    private boolean deletable;
}

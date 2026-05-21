package com.enrollment.student.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentResponse {
    private Long id;
    private String cnie;
    private String firstName;
    private String lastName;
    private String email;
}

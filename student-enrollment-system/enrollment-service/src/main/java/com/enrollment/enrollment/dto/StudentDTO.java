package com.enrollment.enrollment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String cnie;
    private String firstName;
    private String lastName;
    private String email;
}

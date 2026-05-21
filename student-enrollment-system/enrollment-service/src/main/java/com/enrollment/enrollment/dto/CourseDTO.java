package com.enrollment.enrollment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private int credits;
}

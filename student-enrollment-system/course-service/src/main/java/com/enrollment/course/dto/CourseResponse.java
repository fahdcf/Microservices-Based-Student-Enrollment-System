package com.enrollment.course.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private int credits;
}

package com.enrollment.course.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000)
    private String description;

    @Min(1) @Max(10)
    private int credits;
}

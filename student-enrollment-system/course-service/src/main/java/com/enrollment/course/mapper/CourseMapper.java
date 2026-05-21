package com.enrollment.course.mapper;

import com.enrollment.course.dto.*;
import com.enrollment.course.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public Course toEntity(CourseRequest request) {
        return Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .credits(request.getCredits())
                .build();
    }

    public CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .credits(course.getCredits())
                .build();
    }
}

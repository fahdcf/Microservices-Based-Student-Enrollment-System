package com.enrollment.enrollment.client;

import com.enrollment.enrollment.dto.CourseDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Component
public class CourseClient {
    private final RestClient client;
    public CourseClient(RestClient.Builder builder,
            @org.springframework.beans.factory.annotation.Value("${course.service.url:http://course-service}") String url) {
        client = builder.clone().baseUrl(url).build();
    }

    public CourseDTO getCourseById(Long id) {
        try {
            CourseDTO result = client.get().uri("/api/courses/{value}", id).retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new ResponseStatusException(NOT_FOUND, "Course not found");
                }).body(CourseDTO.class);
            if (result == null || result.getId() == null || result.getTitle() == null || !result.getId().equals(id)) {
                throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Invalid response from Course Service");
            }
            return result;
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Course Service unavailable");
        }
    }
}

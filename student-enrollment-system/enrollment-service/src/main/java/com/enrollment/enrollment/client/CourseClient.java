package com.enrollment.enrollment.client;

import com.enrollment.enrollment.dto.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CourseClient {

    @Qualifier("courseWebClient")
    private final WebClient courseWebClient;

    public CourseDTO getCourseById(Long id) {
        return courseWebClient.get()
                .uri("/api/courses/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Course not found with id: " + id)))
                .bodyToMono(CourseDTO.class)
                .block();
    }
}

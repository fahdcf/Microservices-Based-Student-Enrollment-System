package com.enrollment.student.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class EnrollmentClient {
    private final RestClient client;
    public EnrollmentClient(RestClient.Builder builder,
            @org.springframework.beans.factory.annotation.Value("${enrollment.service.url:http://enrollment-service}") String url) {
        client = builder.clone().baseUrl(url).build();
    }

    // Closing a reference is idempotent and blocks future enrollments before local deletion.
    public void closeStudentForEnrollment(Long id) {
        try {
            client.post().uri("/internal/references/students/{id}/close", id).retrieve()
                .onStatus(status -> status.value() == 409, (request, response) -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete student with active enrollments");
                }).toBodilessEntity();
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Enrollment Service unavailable; deletion was not completed");
        }
    }
}

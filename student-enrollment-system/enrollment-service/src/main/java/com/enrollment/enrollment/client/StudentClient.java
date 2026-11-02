package com.enrollment.enrollment.client;

import com.enrollment.enrollment.dto.StudentDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Component
public class StudentClient {
    private final RestClient client;
    public StudentClient(RestClient.Builder builder,
            @org.springframework.beans.factory.annotation.Value("${student.service.url:http://student-service}") String url) {
        client = builder.clone().baseUrl(url).build();
    }

    public StudentDTO getStudentByCnie(String cnie) {
        try {
            StudentDTO result = client.get().uri("/api/students/cnie/{value}", cnie).retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new ResponseStatusException(NOT_FOUND, "Student not found");
                }).body(StudentDTO.class);
            if (result == null || result.getId() == null || result.getCnie() == null) {
                throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Invalid response from Student Service");
            }
            return result;
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Student Service unavailable");
        }
    }
}

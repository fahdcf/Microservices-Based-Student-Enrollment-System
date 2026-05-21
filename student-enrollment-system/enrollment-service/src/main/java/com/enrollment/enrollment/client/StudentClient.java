package com.enrollment.enrollment.client;

import com.enrollment.enrollment.dto.StudentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StudentClient {

    @Qualifier("studentWebClient")
    private final WebClient studentWebClient;

    public StudentDTO getStudentByCnie(String cnie) {
        return studentWebClient.get()
                .uri("/api/students/cnie/{cnie}", cnie)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Student not found with CNIE: " + cnie)))
                .bodyToMono(StudentDTO.class)
                .block();
    }

    public StudentDTO getStudentById(Long id) {
        return studentWebClient.get()
                .uri("/api/students/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new IllegalArgumentException("Student not found with id: " + id)))
                .bodyToMono(StudentDTO.class)
                .block();
    }
}

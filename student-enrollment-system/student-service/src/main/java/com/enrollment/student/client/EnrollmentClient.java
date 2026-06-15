package com.enrollment.student.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EnrollmentClient {

    private final WebClient webClient;

    public EnrollmentClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://enrollment-service").build();
    }

    public void deleteEnrollmentsByStudentId(Long studentId) {
        webClient.delete()
                .uri("/api/enrollments/student/{studentId}", studentId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
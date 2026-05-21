package com.enrollment.enrollment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${student.service.url}")
    private String studentServiceUrl;

    @Value("${course.service.url}")
    private String courseServiceUrl;

    @Bean
    public WebClient studentWebClient() {
        return WebClient.builder()
                .baseUrl(studentServiceUrl)
                .build();
    }

    @Bean
    public WebClient courseWebClient() {
        return WebClient.builder()
                .baseUrl(courseServiceUrl)
                .build();
    }
}

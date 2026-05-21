package com.enrollment.course.config;

import org.springframework.context.annotation.Configuration;

// CORS is handled exclusively by the API Gateway (port 8080).
// Defining it here too would cause duplicate Access-Control-Allow-Origin headers.
@Configuration
public class CorsConfig {
}

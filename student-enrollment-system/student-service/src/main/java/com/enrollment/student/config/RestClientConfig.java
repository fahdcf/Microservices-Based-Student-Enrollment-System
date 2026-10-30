package com.enrollment.student.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    @LoadBalanced
    @ConditionalOnProperty(name = "clients.discovery.enabled", havingValue = "true", matchIfMissing = true)
    RestClient.Builder serviceRestClientBuilder(
            @Value("${clients.connect-timeout}") Duration connectTimeout,
            @Value("${clients.read-timeout}") Duration readTimeout) {
        return createBuilder(connectTimeout, readTimeout);
    }

    @Bean
    @ConditionalOnProperty(name = "clients.discovery.enabled", havingValue = "false")
    RestClient.Builder directRestClientBuilder(
            @Value("${clients.connect-timeout}") Duration connectTimeout,
            @Value("${clients.read-timeout}") Duration readTimeout) {
        return createBuilder(connectTimeout, readTimeout);
    }

    private RestClient.Builder createBuilder(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory).requestInterceptor((request, body, execution) -> {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
                request.getHeaders().setBearerAuth(auth.getToken().getTokenValue());
            }
            return execution.execute(request, body);
        });
    }
}

package com.example.gym.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VerxorConfiguration {

    @Value("${verxor.project.id}")
    private String projectId;

    @Value("${verxor.api.url}")
    private String apiUrl;

    @Bean
    public WebClient verxorWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getProjectId() {
        return projectId;
    }
}

package com.example.chatgpt.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "openai.api")
@Data
@Slf4j
public class OpenAiConfig {
    private String key;
    private String url;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    
    @PostConstruct
    public void init() {
        log.info("=== OpenAI Configuration ===");
        log.info("URL: {}", url);
        log.info("Model: {}", model);
        log.info("Temperature: {}", temperature);
        log.info("Max Tokens: {}", maxTokens);
        
        if (key == null || key.isEmpty() || key.equals("your-api-key-here")) {
            log.error("⚠️  OpenAI API Key가 설정되지 않았습니다!");
            log.error("⚠️  환경 변수를 설정해주세요: export OPENAI_API_KEY='your-key'");
        } else {
            log.info("API Key: {}...{} ({}자)", 
                key.substring(0, Math.min(7, key.length())),
                key.length() > 10 ? key.substring(key.length() - 4) : "",
                key.length());
        }
        log.info("===========================");
    }
}
package com.example.chatgpt.runner;

import com.example.chatgpt.service.DataProcessingService;
import com.example.chatgpt.service.OpenAiService;
import com.example.chatgpt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestRunner implements CommandLineRunner {

    private final OpenAiService openAiService;
    private final PromptTemplateService promptTemplateService;
    private final DataProcessingService dataProcessingService;

    @Override
    public void run(String... args) throws Exception {
        log.info("========== ChatGPT Integration Test Started ==========");
        
        // 주석을 해제하여 테스트 실행
        
        // testSimpleChat();
        // testDataExtraction();
        // testSentimentAnalysis();
        
        log.info("========== ChatGPT Integration Test Completed ==========");
    }

    private void testSimpleChat() {
        log.info("--- Test 1: Simple Chat ---");
        String response = openAiService.chat("안녕하세요! Spring Boot와 ChatGPT 연동 테스트입니다.");
        log.info("Response: {}", response);
    }

    private void testDataExtraction() {
        log.info("--- Test 2: Data Extraction ---");
        String text = """
                홍길동 고객님의 주문 정보입니다.
                주문번호: ORD-2024-001
                이메일: hong@example.com
                전화번호: 010-1234-5678
                주소: 서울시 강남구
                주문금액: 50,000원
                """;
        
        List<String> fields = Arrays.asList("name", "orderNumber", "email", "phone", "address", "amount");
        Map result = dataProcessingService.extractStructuredData(text, fields);
        log.info("Extraction Result: {}", result);
    }

    private void testSentimentAnalysis() {
        log.info("--- Test 3: Sentiment Analysis ---");
        String text = "이 제품은 정말 훌륭합니다! 품질도 좋고 가격도 합리적이에요. 강력 추천합니다.";
        String prompt = promptTemplateService.createSentimentAnalysisPrompt(text);
        String response = openAiService.chat(prompt);
        log.info("Sentiment Analysis: {}", response);
    }
}

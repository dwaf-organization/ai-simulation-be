package com.example.chatgpt.service;

import com.example.chatgpt.config.OpenAiConfig;
import com.example.chatgpt.dto.OpenAiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OpenAiService {

    private final OpenAiConfig openAiConfig;
    private final WebClient webClient;

    public OpenAiService(OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.webClient = WebClient.builder()
                .baseUrl(openAiConfig.getUrl())
                .defaultHeader("Authorization", "Bearer " + openAiConfig.getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 단순 프롬프트 실행
     */
    public String chat(String prompt) {
        List<OpenAiDto.Message> messages = new ArrayList<>();
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(prompt)
                .build());

        return executeChat(messages);
    }

    /**
     * 시스템 메시지와 함께 프롬프트 실행
     */
    public String chatWithSystem(String systemMessage, String userMessage) {
        List<OpenAiDto.Message> messages = new ArrayList<>();
        messages.add(OpenAiDto.Message.builder()
                .role("system")
                .content(systemMessage)
                .build());
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        return executeChat(messages);
    }

    /**
     * 대화 히스토리와 함께 프롬프트 실행
     */
    public String chatWithHistory(List<OpenAiDto.Message> conversationHistory, String userMessage) {
        List<OpenAiDto.Message> messages = new ArrayList<>(conversationHistory);
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        return executeChat(messages);
    }

    /**
     * ChatGPT API 실행
     */
    private String executeChat(List<OpenAiDto.Message> messages) {
        OpenAiDto.ChatRequest request = OpenAiDto.ChatRequest.builder()
                .model(openAiConfig.getModel())
                .messages(messages)
                .temperature(openAiConfig.getTemperature())
                .maxTokens(openAiConfig.getMaxTokens())
                .build();

        log.debug("Sending request to OpenAI: {}", request);

        int maxRetries = 1;  // Free 티어: 재시도 1번만
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                OpenAiDto.ChatResponse response = webClient.post()
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(
                            status -> status.value() == 429,
                            clientResponse -> {
                                // Rate Limit 정보 추출
                                String rateLimitLimit = clientResponse.headers().asHttpHeaders().getFirst("x-ratelimit-limit-requests");
                                String rateLimitRemaining = clientResponse.headers().asHttpHeaders().getFirst("x-ratelimit-remaining-requests");
                                String rateLimitReset = clientResponse.headers().asHttpHeaders().getFirst("x-ratelimit-reset-requests");
                                
                                // 추가 헤더들
                                String rateLimitLimitTokens = clientResponse.headers().asHttpHeaders().getFirst("x-ratelimit-limit-tokens");
                                String rateLimitRemainingTokens = clientResponse.headers().asHttpHeaders().getFirst("x-ratelimit-remaining-tokens");
                                String retryAfter = clientResponse.headers().asHttpHeaders().getFirst("retry-after");
                                
                                log.warn("========== Rate Limit 상세 정보 ==========");
                                log.warn("요청 한도: {}", rateLimitLimit);
                                log.warn("남은 요청: {}", rateLimitRemaining);
                                log.warn("리셋 시간: {}", rateLimitReset);
                                log.warn("토큰 한도: {}", rateLimitLimitTokens);
                                log.warn("남은 토큰: {}", rateLimitRemainingTokens);
                                log.warn("Retry-After: {} 초", retryAfter);
                                log.warn("현재 시각: {}", java.time.LocalDateTime.now());
                                log.warn("==========================================");
                                
                                return clientResponse.createException();
                            }
                        )
                        .bodyToMono(OpenAiDto.ChatResponse.class)
                        .block();

                if (response != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.debug("Received response from OpenAI. Tokens used: {}", response.getUsage().getTotalTokens());
                    return content;
                }

                throw new RuntimeException("No response from OpenAI");
                
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
                retryCount++;
                
                // Free 티어는 분당 3회 제한이므로 2분 대기 (확실하게)
                int waitSeconds = 120; // 2분 대기 (Free 티어용)
                
                if (retryCount >= maxRetries) {
                    log.error("========================================");
                    log.error("⚠️  OpenAI Rate Limit 초과!");
                    log.error("========================================");
                    log.error("📌 Free 티어는 분당 3회로 제한됩니다.");
                    log.error("📌 최소 2-3분 기다린 후 다시 시도해주세요.");
                    log.error("");
                    log.error("💡 해결 방법:");
                    log.error("   1) 지금: 3분 기다리기");
                    log.error("   2) 근본 해결: 결제 정보 등록 (분당 3회 → 500회)");
                    log.error("      https://platform.openai.com/settings/organization/billing");
                    log.error("      실제 비용: 1회 약 2원, 월 $1-2 정도");
                    log.error("========================================");
                    throw new RuntimeException("⏰ Rate Limit 초과! 2-3분 후 다시 시도해주세요. (Free 티어: 분당 3회)", e);
                }
                
                log.warn("⏰ Rate Limit 초과. {}초 후 재시도... ({}/{})", waitSeconds, retryCount, maxRetries);
                log.warn("💡 Free 티어는 분당 3회로 제한됩니다.");
                
                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 중단됨", ie);
                }
                
            } catch (Exception e) {
                log.error("OpenAI API 호출 실패: {}", e.getMessage());
                throw new RuntimeException("ChatGPT API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }
        
        throw new RuntimeException("최대 재시도 횟수 초과");
    }

    /**
     * 전체 응답 객체 반환 (토큰 사용량 등 메타데이터 필요시)
     */
    public OpenAiDto.ChatResponse chatWithFullResponse(String prompt) {
        List<OpenAiDto.Message> messages = new ArrayList<>();
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(prompt)
                .build());

        OpenAiDto.ChatRequest request = OpenAiDto.ChatRequest.builder()
                .model(openAiConfig.getModel())
                .messages(messages)
                .temperature(openAiConfig.getTemperature())
                .maxTokens(openAiConfig.getMaxTokens())
                .build();

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiDto.ChatResponse.class)
                .block();
    }
}
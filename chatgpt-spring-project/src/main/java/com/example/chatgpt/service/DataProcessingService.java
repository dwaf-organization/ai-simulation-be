package com.example.chatgpt.service;

import com.example.chatgpt.dto.OpenAiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
public class DataProcessingService {

    private final OpenAiService openAiService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * CSV 데이터 분석
     */
    public Map<String, Object> analyzeCSVData(String csvData) {
        String prompt = promptTemplateService.createDataAnalysisPrompt(csvData, "CSV 데이터 분석");
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        result.put("analysis", response);
        result.put("timestamp", new Date());
        return result;
    }

    /**
     * 텍스트에서 구조화된 데이터 추출
     */
    public Map<String, Object> extractStructuredData(String text, List<String> fields) {
        String prompt = promptTemplateService.createJsonExtractionPrompt(text, fields);
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            // JSON 응답 파싱
            Object jsonData = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("data", jsonData);
            result.put("rawResponse", response);
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 배치 텍스트 처리
     */
    public List<Map<String, Object>> processBatchTexts(List<String> texts, String processingType) {
        return IntStream.range(0, texts.size())
                .mapToObj(index -> {
                    String text = texts.get(index);
                    log.info("Processing batch item {}/{}", index + 1, texts.size());
                    
                    String prompt;
                    switch (processingType) {
                        case "summarize":
                            prompt = promptTemplateService.createSummarizationPrompt(text, 200);
                            break;
                        case "sentiment":
                            prompt = promptTemplateService.createSentimentAnalysisPrompt(text);
                            break;
                        default:
                            prompt = text;
                    }
                    
                    String response = openAiService.chat(prompt);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("index", index);
                    result.put("originalText", text);
                    result.put("processedResult", response);
                    result.put("processingType", processingType);
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 데이터 변환 및 생성
     */
    public Map<String, Object> transformData(Map<String, Object> inputData, String transformationType) {
        try {
            String dataString = objectMapper.writeValueAsString(inputData);
            
            String prompt = String.format("""
다음 데이터를 %s 형식으로 변환해주세요:

%s

변환된 결과만 반환해주세요.
""", transformationType, dataString);
            
            String response = openAiService.chat(prompt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("input", inputData);
            result.put("output", response);
            result.put("transformationType", transformationType);
            result.put("timestamp", new Date());
            return result;
        } catch (Exception e) {
            log.error("Failed to transform data", e);
            throw new RuntimeException("Data transformation failed", e);
        }
    }

    /**
     * Q&A 시스템 - 컨텍스트 기반 질의응답
     */
    public String answerQuestion(String context, String question) {
        String prompt = promptTemplateService.createQAPrompt(context, question);
        return openAiService.chat(prompt);
    }

    /**
     * 대화 히스토리를 유지하면서 대화
     */
    public Map<String, Object> chatWithContext(List<Map<String, String>> conversationHistory, String userMessage) {
        // Map을 OpenAiDto.Message로 변환
        List<OpenAiDto.Message> messages = conversationHistory.stream()
                .map(msg -> OpenAiDto.Message.builder()
                        .role(msg.get("role"))
                        .content(msg.get("content"))
                        .build())
                .collect(Collectors.toList());
        
        String response = openAiService.chatWithHistory(messages, userMessage);
        
        // 새로운 메시지를 히스토리에 추가
        List<Map<String, String>> updatedHistory = new ArrayList<>(conversationHistory);
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        updatedHistory.add(userMsg);
        
        Map<String, String> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", response);
        updatedHistory.add(assistantMsg);
        
        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("conversationHistory", updatedHistory);
        return result;
    }

    /**
     * 데이터 분류
     */
    public Map<String, Object> classifyData(String text, List<String> categories) {
        String categoryList = String.join(", ", categories);
        String prompt = String.format("""
다음 텍스트를 분류해주세요.

가능한 카테고리: %s

텍스트:
%s

다음 JSON 형식으로 응답해주세요:
{
  "category": "선택된 카테고리",
  "confidence": "신뢰도 (0-1)",
  "reason": "분류 이유"
}
""", categoryList, text);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object classification = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("classification", classification);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 데이터 검증 및 교정
     */
    public Map<String, Object> validateAndCorrectData(String data, List<String> validationRules) {
        String rules = validationRules.stream()
                .map(rule -> "- " + rule)
                .collect(Collectors.joining("\n"));
        
        String prompt = String.format("""
다음 데이터를 검증하고 필요시 교정해주세요.

검증 규칙:
%s

데이터:
%s

다음 JSON 형식으로 응답해주세요:
{
  "isValid": true/false,
  "errors": ["에러 목록"],
  "correctedData": "교정된 데이터 (필요시)",
  "suggestions": ["개선 제안"]
}
""", rules, data);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object validation = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("validation", validation);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }
}

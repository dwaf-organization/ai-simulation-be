package com.example.chatgpt.controller;

import com.example.chatgpt.dto.OpenAiDto;
import com.example.chatgpt.service.DataProcessingService;
import com.example.chatgpt.service.OpenAiService;
import com.example.chatgpt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatgpt")
@RequiredArgsConstructor
public class ChatGptController {

    private final OpenAiService openAiService;
    private final PromptTemplateService promptTemplateService;
    private final DataProcessingService dataProcessingService;

    /**
     * 단순 채팅
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String response = openAiService.chat(prompt);
        return ResponseEntity.ok(Map.of("response", response));
    }

    /**
     * 시스템 메시지와 함께 채팅
     */
    @PostMapping("/chat/system")
    public ResponseEntity<Map<String, String>> chatWithSystem(@RequestBody Map<String, String> request) {
        String systemMessage = request.get("systemMessage");
        String userMessage = request.get("userMessage");
        String response = openAiService.chatWithSystem(systemMessage, userMessage);
        return ResponseEntity.ok(Map.of("response", response));
    }

    /**
     * 전체 응답 정보 (토큰 사용량 포함)
     */
    @PostMapping("/chat/full")
    public ResponseEntity<OpenAiDto.ChatResponse> chatWithFullResponse(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        OpenAiDto.ChatResponse response = openAiService.chatWithFullResponse(prompt);
        return ResponseEntity.ok(response);
    }

    /**
     * 텍스트 요약
     */
    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarize(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        Integer maxLength = (Integer) request.getOrDefault("maxLength", 200);
        
        String prompt = promptTemplateService.createSummarizationPrompt(text, maxLength);
        String response = openAiService.chat(prompt);
        
        return ResponseEntity.ok(Map.of("summary", response));
    }

    /**
     * 데이터 분석
     */
    @PostMapping("/analyze/csv")
    public ResponseEntity<Map> analyzeCSV(@RequestBody Map<String, String> request) {
        String csvData = request.get("data");
        Map result = dataProcessingService.analyzeCSVData(csvData);
        return ResponseEntity.ok(result);
    }

    /**
     * 구조화된 데이터 추출
     */
    @PostMapping("/extract")
    public ResponseEntity<Map> extractData(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) request.get("fields");
        
        Map result = dataProcessingService.extractStructuredData(text, fields);
        return ResponseEntity.ok(result);
    }

    /**
     * 배치 텍스트 처리
     */
    @PostMapping("/batch")
    public ResponseEntity<List<Map<String, Object>>> processBatch(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) request.get("texts");
        String processingType = (String) request.getOrDefault("type", "summarize");
        
        List<Map<String, Object>> results = dataProcessingService.processBatchTexts(texts, processingType);
        return ResponseEntity.ok(results);
    }

    /**
     * Q&A
     */
    @PostMapping("/qa")
    public ResponseEntity<Map<String, String>> questionAnswer(@RequestBody Map<String, String> request) {
        String context = request.get("context");
        String question = request.get("question");
        
        String answer = dataProcessingService.answerQuestion(context, question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    /**
     * 대화 (히스토리 유지)
     */
    @PostMapping("/conversation")
    public ResponseEntity<Map<String, Object>> conversation(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) request.getOrDefault("history", List.of());
        String message = (String) request.get("message");
        
        Map<String, Object> result = dataProcessingService.chatWithContext(history, message);
        return ResponseEntity.ok(result);
    }

    /**
     * 데이터 분류
     */
    @PostMapping("/classify")
    public ResponseEntity<Map> classify(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) request.get("categories");
        
        Map result = dataProcessingService.classifyData(text, categories);
        return ResponseEntity.ok(result);
    }

    /**
     * 코드 생성
     */
    @PostMapping("/code/generate")
    public ResponseEntity<Map<String, String>> generateCode(@RequestBody Map<String, Object> request) {
        String language = (String) request.get("language");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> requirements = (List<String>) request.getOrDefault("requirements", List.of());
        
        String prompt = promptTemplateService.createCodeGenerationPrompt(language, description, requirements);
        String code = openAiService.chat(prompt);
        
        return ResponseEntity.ok(Map.of("code", code));
    }

    /**
     * 번역
     */
    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String sourceLang = request.getOrDefault("sourceLang", "한국어");
        String targetLang = request.getOrDefault("targetLang", "영어");
        
        String prompt = promptTemplateService.createTranslationPrompt(text, sourceLang, targetLang);
        String translation = openAiService.chat(prompt);
        
        return ResponseEntity.ok(Map.of("translation", translation));
    }

    /**
     * 감정 분석
     */
    @PostMapping("/sentiment")
    public ResponseEntity<Map<String, String>> analyzeSentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        String prompt = promptTemplateService.createSentimentAnalysisPrompt(text);
        String analysis = openAiService.chat(prompt);
        
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    /**
     * 데이터 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<Map> validateData(@RequestBody Map<String, Object> request) {
        String data = (String) request.get("data");
        @SuppressWarnings("unchecked")
        List<String> rules = (List<String>) request.get("rules");
        
        Map result = dataProcessingService.validateAndCorrectData(data, rules);
        return ResponseEntity.ok(result);
    }
}

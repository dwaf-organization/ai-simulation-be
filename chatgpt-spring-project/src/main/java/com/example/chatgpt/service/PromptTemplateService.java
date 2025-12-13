package com.example.chatgpt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@Slf4j
public class PromptTemplateService {

    /**
     * 템플릿과 변수를 바인딩하여 프롬프트 생성
     */
    public String generatePrompt(String template, Map<String, Object> variables) {
        try {
            String result = template;
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    String placeholder = "${" + entry.getKey() + "}";
                    result = result.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }
            log.debug("Generated prompt from template: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to generate prompt from template", e);
            throw new RuntimeException("Failed to generate prompt", e);
        }
    }

    /**
     * 데이터 분석 프롬프트 생성
     */
    public String createDataAnalysisPrompt(String data, String analysisType) {
        return String.format("""
당신은 데이터 분석 전문가입니다. 다음 데이터를 분석해주세요.

분석 유형: %s

데이터:
%s

다음 형식으로 분석 결과를 제공해주세요:
1. 주요 발견사항
2. 통계적 인사이트
3. 권장사항
""", analysisType, data);
    }

    /**
     * 텍스트 요약 프롬프트 생성
     */
    public String createSummarizationPrompt(String text, int maxLength) {
        return String.format("""
다음 텍스트를 %d자 이내로 요약해주세요:

%s
""", maxLength, text);
    }

    /**
     * JSON 데이터 추출 프롬프트 생성
     */
    public String createJsonExtractionPrompt(String text, List<String> fields) {
        String fieldList = String.join(", ", fields);
        return String.format("""
다음 텍스트에서 정보를 추출하여 JSON 형식으로 반환해주세요.

추출할 필드: %s

텍스트:
%s

JSON 형식으로만 응답해주세요 (다른 설명 없이).
""", fieldList, text);
    }

    /**
     * 질의응답 프롬프트 생성
     */
    public String createQAPrompt(String context, String question) {
        return String.format("""
다음 컨텍스트를 참고하여 질문에 답변해주세요.

컨텍스트:
%s

질문: %s

답변:
""", context, question);
    }

    /**
     * 코드 생성 프롬프트 생성
     */
    public String createCodeGenerationPrompt(String language, String description, List<String> requirements) {
        String reqList = "";
        if (requirements != null && !requirements.isEmpty()) {
            reqList = "\n요구사항:\n" + requirements.stream()
                    .map(req -> "- " + req)
                    .collect(Collectors.joining("\n"));
        }
        return String.format("""
%s 언어로 다음 기능을 구현하는 코드를 작성해주세요.

설명: %s%s

코드만 작성해주세요 (주석 포함).
""", language, description, reqList);
    }

    /**
     * 번역 프롬프트 생성
     */
    public String createTranslationPrompt(String text, String sourceLang, String targetLang) {
        return String.format("""
다음 텍스트를 %s에서 %s로 번역해주세요:

%s
""", sourceLang, targetLang, text);
    }

    /**
     * 감정 분석 프롬프트 생성
     */
    public String createSentimentAnalysisPrompt(String text) {
        return String.format("""
다음 텍스트의 감정을 분석해주세요:

%s

다음 형식으로 응답해주세요:
- 전반적 감정: (긍정/부정/중립)
- 감정 강도: (1-10)
- 주요 감정 키워드:
""", text);
    }
}

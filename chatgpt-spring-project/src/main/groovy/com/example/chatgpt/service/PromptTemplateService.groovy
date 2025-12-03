package com.example.chatgpt.service

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class PromptTemplateService {

    private final SimpleTemplateEngine templateEngine = new SimpleTemplateEngine()

    /**
     * 템플릿과 변수를 바인딩하여 프롬프트 생성
     */
    String generatePrompt(String template, Map<String, Object> variables) {
        try {
            def binding = variables ?: [:]
            def result = templateEngine.createTemplate(template).make(binding).toString()
            log.debug("Generated prompt from template: {}", result)
            return result
        } catch (Exception e) {
            log.error("Failed to generate prompt from template", e)
            throw new RuntimeException("Failed to generate prompt", e)
        }
    }

    /**
     * 데이터 분석 프롬프트 생성
     */
    String createDataAnalysisPrompt(String data, String analysisType) {
        def template = """
당신은 데이터 분석 전문가입니다. 다음 데이터를 분석해주세요.

분석 유형: ${analysisType}

데이터:
${data}

다음 형식으로 분석 결과를 제공해주세요:
1. 주요 발견사항
2. 통계적 인사이트
3. 권장사항
"""
        return template
    }

    /**
     * 텍스트 요약 프롬프트 생성
     */
    String createSummarizationPrompt(String text, int maxLength) {
        def template = """
다음 텍스트를 ${maxLength}자 이내로 요약해주세요:

${text}
"""
        return template
    }

    /**
     * JSON 데이터 추출 프롬프트 생성
     */
    String createJsonExtractionPrompt(String text, List<String> fields) {
        def fieldList = fields.join(", ")
        def template = """
다음 텍스트에서 정보를 추출하여 JSON 형식으로 반환해주세요.

추출할 필드: ${fieldList}

텍스트:
${text}

JSON 형식으로만 응답해주세요 (다른 설명 없이).
"""
        return template
    }

    /**
     * 질의응답 프롬프트 생성
     */
    String createQAPrompt(String context, String question) {
        def template = """
다음 컨텍스트를 참고하여 질문에 답변해주세요.

컨텍스트:
${context}

질문: ${question}

답변:
"""
        return template
    }

    /**
     * 코드 생성 프롬프트 생성
     */
    String createCodeGenerationPrompt(String language, String description, List<String> requirements = []) {
        def reqList = requirements ? "\n요구사항:\n" + requirements.collect { "- $it" }.join("\n") : ""
        def template = """
${language} 언어로 다음 기능을 구현하는 코드를 작성해주세요.

설명: ${description}${reqList}

코드만 작성해주세요 (주석 포함).
"""
        return template
    }

    /**
     * 번역 프롬프트 생성
     */
    String createTranslationPrompt(String text, String sourceLang, String targetLang) {
        def template = """
다음 텍스트를 ${sourceLang}에서 ${targetLang}로 번역해주세요:

${text}
"""
        return template
    }

    /**
     * 감정 분석 프롬프트 생성
     */
    String createSentimentAnalysisPrompt(String text) {
        def template = """
다음 텍스트의 감정을 분석해주세요:

${text}

다음 형식으로 응답해주세요:
- 전반적 감정: (긍정/부정/중립)
- 감정 강도: (1-10)
- 주요 감정 키워드:
"""
        return template
    }
}

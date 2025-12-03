package com.example.chatgpt.service

import com.example.chatgpt.dto.OpenAiDto
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Slf4j
class DataProcessingService {

    @Autowired
    OpenAiService openAiService

    @Autowired
    PromptTemplateService promptTemplateService

    private final ObjectMapper objectMapper = new ObjectMapper()
    private final JsonSlurper jsonSlurper = new JsonSlurper()

    /**
     * CSV 데이터 분석
     */
    Map analyzeCSVData(String csvData) {
        def prompt = promptTemplateService.createDataAnalysisPrompt(csvData, "CSV 데이터 분석")
        def response = openAiService.chat(prompt)
        
        return [
            analysis: response,
            timestamp: new Date()
        ]
    }

    /**
     * 텍스트에서 구조화된 데이터 추출
     */
    Map extractStructuredData(String text, List<String> fields) {
        def prompt = promptTemplateService.createJsonExtractionPrompt(text, fields)
        def response = openAiService.chat(prompt)
        
        try {
            // JSON 응답 파싱
            def jsonData = jsonSlurper.parseText(response)
            return [
                success: true,
                data: jsonData,
                rawResponse: response
            ]
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e)
            return [
                success: false,
                error: e.message,
                rawResponse: response
            ]
        }
    }

    /**
     * 배치 텍스트 처리
     */
    List<Map> processBatchTexts(List<String> texts, String processingType) {
        def results = []
        
        texts.eachWithIndex { text, index ->
            log.info("Processing batch item ${index + 1}/${texts.size()}")
            
            def prompt = switch(processingType) {
                case 'summarize' -> promptTemplateService.createSummarizationPrompt(text, 200)
                case 'sentiment' -> promptTemplateService.createSentimentAnalysisPrompt(text)
                default -> text
            }
            
            def response = openAiService.chat(prompt)
            
            results << [
                index: index,
                originalText: text,
                processedResult: response,
                processingType: processingType
            ]
        }
        
        return results
    }

    /**
     * 데이터 변환 및 생성
     */
    Map transformData(Map inputData, String transformationType) {
        def dataString = objectMapper.writeValueAsString(inputData)
        
        def prompt = """
다음 데이터를 ${transformationType} 형식으로 변환해주세요:

${dataString}

변환된 결과만 반환해주세요.
"""
        
        def response = openAiService.chat(prompt)
        
        return [
            input: inputData,
            output: response,
            transformationType: transformationType,
            timestamp: new Date()
        ]
    }

    /**
     * Q&A 시스템 - 컨텍스트 기반 질의응답
     */
    String answerQuestion(String context, String question) {
        def prompt = promptTemplateService.createQAPrompt(context, question)
        return openAiService.chat(prompt)
    }

    /**
     * 대화 히스토리를 유지하면서 대화
     */
    Map chatWithContext(List<Map> conversationHistory, String userMessage) {
        // Map을 OpenAiDto.Message로 변환
        def messages = conversationHistory.collect { 
            OpenAiDto.Message.builder()
                .role(it.role as String)
                .content(it.content as String)
                .build()
        }
        
        def response = openAiService.chatWithHistory(messages, userMessage)
        
        // 새로운 메시지를 히스토리에 추가
        def updatedHistory = conversationHistory + [
            [role: 'user', content: userMessage],
            [role: 'assistant', content: response]
        ]
        
        return [
            response: response,
            conversationHistory: updatedHistory
        ]
    }

    /**
     * 데이터 분류
     */
    Map classifyData(String text, List<String> categories) {
        def categoryList = categories.join(", ")
        def prompt = """
다음 텍스트를 분류해주세요.

가능한 카테고리: ${categoryList}

텍스트:
${text}

다음 JSON 형식으로 응답해주세요:
{
  "category": "선택된 카테고리",
  "confidence": "신뢰도 (0-1)",
  "reason": "분류 이유"
}
"""
        
        def response = openAiService.chat(prompt)
        
        try {
            def result = jsonSlurper.parseText(response)
            return [
                success: true,
                classification: result
            ]
        } catch (Exception e) {
            return [
                success: false,
                rawResponse: response
            ]
        }
    }

    /**
     * 데이터 검증 및 교정
     */
    Map validateAndCorrectData(String data, List<String> validationRules) {
        def rules = validationRules.join("\n- ")
        def prompt = """
다음 데이터를 검증하고 필요시 교정해주세요.

검증 규칙:
- ${rules}

데이터:
${data}

다음 JSON 형식으로 응답해주세요:
{
  "isValid": true/false,
  "errors": ["에러 목록"],
  "correctedData": "교정된 데이터 (필요시)",
  "suggestions": ["개선 제안"]
}
"""
        
        def response = openAiService.chat(prompt)
        
        try {
            def result = jsonSlurper.parseText(response)
            return [
                success: true,
                validation: result
            ]
        } catch (Exception e) {
            return [
                success: false,
                rawResponse: response
            ]
        }
    }
}

package com.example.chatgpt.examples;

import com.example.chatgpt.service.DataProcessingService;
import com.example.chatgpt.service.OpenAiService;
import com.example.chatgpt.service.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ChatGPT 활용 고급 예제
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdvancedExamples {

    private final OpenAiService openAiService;
    private final PromptTemplateService promptTemplateService;
    private final DataProcessingService dataProcessingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 예제 1: 상품 리뷰 분석 및 요약
     */
    public Map<String, Object> analyzeProductReviews(List<String> reviews) {
        String allReviews = IntStream.range(0, reviews.size())
                .mapToObj(idx -> String.format("%d. %s", idx + 1, reviews.get(idx)))
                .collect(Collectors.joining("\n"));
        
        String prompt = String.format("""
다음은 고객들의 상품 리뷰입니다. 이를 분석하여 다음 정보를 JSON 형식으로 제공해주세요:

리뷰:
%s

JSON 형식:
{
  "overallSentiment": "긍정/부정/중립",
  "averageRating": "예상 평점 (1-5)",
  "positivePoints": ["긍정적 요소들"],
  "negativePoints": ["부정적 요소들"],
  "keyInsights": ["주요 인사이트"],
  "recommendation": "전반적 추천 여부"
}
""", allReviews);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object analysis = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("analysis", analysis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 예제 2: 회의록 자동 생성
     */
    public String generateMeetingMinutes(Map<String, Object> meetingInfo) {
        String attendees = meetingInfo.containsKey("attendees") 
                ? String.join(", ", (List<String>) meetingInfo.get("attendees"))
                : "";
        
        String prompt = String.format("""
다음 회의 정보를 바탕으로 전문적인 회의록을 작성해주세요:

회의 제목: %s
참석자: %s
날짜: %s

논의 내용:
%s

다음 형식으로 작성해주세요:
1. 회의 개요
2. 주요 논의사항
3. 결정사항
4. 액션 아이템 (담당자 포함)
5. 다음 회의 안건
""", 
                meetingInfo.get("title"),
                attendees,
                meetingInfo.get("date"),
                meetingInfo.get("discussion"));
        
        return openAiService.chat(prompt);
    }

    /**
     * 예제 3: 코드 리뷰 및 개선 제안
     */
    public Map<String, Object> reviewCode(String code, String language) {
        String prompt = String.format("""
다음 %s 코드를 리뷰하고 개선 제안을 해주세요:

```%s
%s
```

다음 JSON 형식으로 응답해주세요:
{
  "codeQuality": "점수 (1-10)",
  "strengths": ["강점들"],
  "weaknesses": ["약점들"],
  "securityIssues": ["보안 이슈들"],
  "performanceIssues": ["성능 이슈들"],
  "improvements": [
    {
      "issue": "문제점",
      "suggestion": "개선 제안",
      "priority": "high/medium/low"
    }
  ],
  "improvedCode": "개선된 코드"
}
""", language, language, code);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object review = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("review", review);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 예제 4: 이메일 자동 작성
     */
    public String composeEmail(Map<String, Object> emailInfo) {
        String tone = emailInfo.getOrDefault("tone", "정중하고 전문적인").toString();
        String additionalNotes = emailInfo.containsKey("additionalNotes")
                ? "\n추가 참고사항: " + emailInfo.get("additionalNotes")
                : "";
        
        String prompt = String.format("""
다음 정보를 바탕으로 전문적인 이메일을 작성해주세요:

받는 사람: %s
목적: %s
톤: %s
주요 내용:
%s%s
""",
                emailInfo.get("recipient"),
                emailInfo.get("purpose"),
                tone,
                emailInfo.get("content"),
                additionalNotes);
        
        return openAiService.chat(prompt);
    }

    /**
     * 예제 5: 데이터베이스 쿼리 생성
     */
    public Map<String, Object> generateDatabaseQuery(String naturalLanguageQuery, String dbType) {
        String prompt = String.format("""
다음 자연어 요청을 %s 쿼리로 변환해주세요:

요청: %s

다음 JSON 형식으로 응답해주세요:
{
  "query": "SQL 쿼리",
  "explanation": "쿼리 설명",
  "assumptions": ["가정한 사항들"],
  "warnings": ["주의사항들"]
}
""", dbType, naturalLanguageQuery);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object queryInfo = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("queryInfo", queryInfo);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 예제 6: 문서 비교 및 차이점 분석
     */
    public String compareDocuments(String doc1, String doc2) {
        String prompt = String.format("""
다음 두 문서를 비교하고 주요 차이점을 분석해주세요:

문서 1:
%s

문서 2:
%s

다음 내용을 포함하여 분석해주세요:
1. 주요 차이점
2. 추가된 내용
3. 삭제된 내용
4. 수정된 내용
5. 전반적인 변경 사항 요약
""", doc1, doc2);
        
        return openAiService.chat(prompt);
    }

    /**
     * 예제 7: 프로젝트 작업 분해 (Work Breakdown)
     */
    public Map<String, Object> breakdownProject(String projectDescription) {
        String prompt = String.format("""
다음 프로젝트를 작업 단위로 분해해주세요:

프로젝트 설명:
%s

다음 JSON 형식으로 응답해주세요:
{
  "phases": [
    {
      "phaseName": "단계명",
      "duration": "예상 기간",
      "tasks": [
        {
          "taskName": "작업명",
          "description": "작업 설명",
          "estimatedHours": "예상 시간",
          "dependencies": ["선행 작업들"],
          "skills": ["필요 스킬들"]
        }
      ]
    }
  ],
  "totalEstimatedHours": "전체 예상 시간",
  "risks": ["리스크 요소들"],
  "recommendations": ["권장사항들"]
}
""", projectDescription);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object breakdown = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("breakdown", breakdown);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 예제 8: 스마트 FAQ 생성
     */
    public List<Map<String, Object>> generateFAQ(String content, int numberOfQuestions) {
        String prompt = String.format("""
다음 내용을 바탕으로 %d개의 FAQ를 생성해주세요:

내용:
%s

다음 JSON 배열 형식으로 응답해주세요:
[
  {
    "question": "질문",
    "answer": "답변",
    "category": "카테고리",
    "difficulty": "쉬움/보통/어려움"
  }
]
""", numberOfQuestions, content);
        
        String response = openAiService.chat(prompt);
        
        try {
            return objectMapper.readValue(response, List.class);
        } catch (Exception e) {
            log.error("Failed to parse FAQ response", e);
            return List.of();
        }
    }

    /**
     * 예제 9: 테스트 케이스 생성
     */
    public Map<String, Object> generateTestCases(String functionDescription, String language) {
        String prompt = String.format("""
다음 함수에 대한 테스트 케이스를 생성해주세요:

언어: %s
함수 설명:
%s

다음 JSON 형식으로 응답해주세요:
{
  "testCases": [
    {
      "name": "테스트 케이스 이름",
      "description": "테스트 설명",
      "input": "입력값",
      "expectedOutput": "예상 출력",
      "testType": "단위/통합/엣지케이스"
    }
  ],
  "testCode": "실제 테스트 코드"
}
""", language, functionDescription);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object testInfo = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("testInfo", testInfo);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }

    /**
     * 예제 10: 데이터 파이프라인 설계
     */
    public Map<String, Object> designDataPipeline(String requirements) {
        String prompt = String.format("""
다음 요구사항을 바탕으로 데이터 파이프라인을 설계해주세요:

요구사항:
%s

다음 JSON 형식으로 응답해주세요:
{
  "pipelineStages": [
    {
      "stageName": "단계명",
      "description": "설명",
      "inputData": "입력 데이터",
      "processing": "처리 방법",
      "outputData": "출력 데이터",
      "tools": ["사용 도구들"]
    }
  ],
  "architecture": "전체 아키텍처 설명",
  "scalability": "확장성 고려사항",
  "monitoring": "모니터링 방안",
  "estimatedCost": "예상 비용 범위"
}
""", requirements);
        
        String response = openAiService.chat(prompt);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Object design = objectMapper.readValue(response, Object.class);
            result.put("success", true);
            result.put("design", design);
        } catch (Exception e) {
            result.put("success", false);
            result.put("rawResponse", response);
        }
        return result;
    }
}

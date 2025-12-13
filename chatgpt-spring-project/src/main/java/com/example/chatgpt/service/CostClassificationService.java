package com.example.chatgpt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
/**
 * 답변을 손익계산서 영업비용 항목으로 분류하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CostClassificationService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 손익계산서 영업비용 항목 (판매비와관리비)
    private static final List<String> EXPENSE_CATEGORIES = Arrays.asList(
        "급여", "퇴직급여", "복리후생비", "임차료", "접대비",
        "광고선전비", "운반비", "통신비", "수도광열비", "세금과공과",
        "감가상각비", "무형자산상각비", "보험료", "차량유지비", "여비교통비",
        "수선비", "소모품비", "도서인쇄비", "교육훈련비", "지급수수료",
        "연구개발비", "기타"
    );

    /**
     * 질문-답변을 분석하여 비용 항목과 금액 추정
     */
    public Map<String, Object> classifyExpense(String question, String answer) {
        try {
            String prompt = buildClassificationPrompt(question, answer);
            String response = openAiService.chat(prompt);
            
            log.info("비용 분류 결과: {}", response);
            
            return parseClassificationResult(response);
            
        } catch (Exception e) {
            log.error("비용 분류 중 오류 발생", e);
            return createDefaultClassification();
        }
    }

    /**
     * 여러 질문-답변을 일괄 분류
     */
    public List<Map<String, Object>> classifyMultipleExpenses(Map<String, String> questionsAndAnswers) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : questionsAndAnswers.entrySet()) {
            String question = entry.getKey();
            String answer = entry.getValue();
            
            Map<String, Object> classification = classifyExpense(question, answer);
            classification.put("question", question);
            classification.put("answer", answer);
            
            results.add(classification);
        }
        
        return results;
    }

    /**
     * ChatGPT 프롬프트 생성
     */
    private String buildClassificationPrompt(String question, String answer) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 회계 전문가입니다. 다음 질문과 답변을 분석하여 ");
        prompt.append("손익계산서의 영업비용(판매비와관리비) 항목으로 분류하고 월 비용을 추정하세요.\n\n");
        
        prompt.append("# 사용 가능한 비용 항목:\n");
        for (String category : EXPENSE_CATEGORIES) {
            prompt.append("- ").append(category).append("\n");
        }
        
        prompt.append("\n# 질문:\n").append(question).append("\n\n");
        prompt.append("# 답변:\n").append(answer).append("\n\n");
        
        prompt.append("# 출력 형식 (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"category\": \"비용항목명\",\n");
        prompt.append("  \"monthlyCost\": 예상월비용(숫자만, 만원단위),\n");
        prompt.append("  \"reasoning\": \"분류 근거 (1줄)\"\n");
        prompt.append("}\n\n");
        
        prompt.append("주의사항:\n");
        prompt.append("1. monthlyCost는 숫자만 입력 (예: 500 = 500만원)\n");
        prompt.append("2. 비용이 발생하지 않는 경우 monthlyCost는 0\n");
        prompt.append("3. category는 위 목록 중 하나를 정확히 선택\n");
        prompt.append("4. 복수 항목이 필요하면 가장 주요한 항목 1개만 선택\n");
        prompt.append("5. JSON 형식만 출력하고 다른 설명은 생략\n");
        
        return prompt.toString();
    }

    /**
     * ChatGPT 응답 파싱
     */
    private Map<String, Object> parseClassificationResult(String response) {
        try {
            // JSON 추출 (```json ``` 제거)
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();
            
            Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            
            // 검증
            String category = (String) result.get("category");
            if (!EXPENSE_CATEGORIES.contains(category)) {
                log.warn("유효하지 않은 카테고리: {}, 기타로 변경", category);
                result.put("category", "기타");
            }
            
            // monthlyCost를 Integer로 변환
            Object costObj = result.get("monthlyCost");
            if (costObj instanceof String) {
                result.put("monthlyCost", Integer.parseInt(((String) costObj).replaceAll("[^0-9]", "")));
            } else if (costObj instanceof Double) {
                result.put("monthlyCost", ((Double) costObj).intValue());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("비용 분류 결과 파싱 실패: {}", response, e);
            return createDefaultClassification();
        }
    }

    /**
     * 기본 분류 (파싱 실패 시)
     */
    private Map<String, Object> createDefaultClassification() {
        Map<String, Object> result = new HashMap<>();
        result.put("category", "기타");
        result.put("monthlyCost", 0);
        result.put("reasoning", "자동 분류 실패 - 수동 입력 필요");
        return result;
    }

    /**
     * Stage별 총 비용 계산
     */
    public Map<String, Object> calculateTotalCost(List<Map<String, Object>> classifications) {
        Map<String, Integer> categoryTotals = new HashMap<>();
        int totalMonthlyCost = 0;
        
        for (Map<String, Object> classification : classifications) {
            String category = (String) classification.get("category");
            Integer monthlyCost = (Integer) classification.get("monthlyCost");
            
            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0) + monthlyCost);
            totalMonthlyCost += monthlyCost;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("categoryTotals", categoryTotals);
        result.put("totalMonthlyCost", totalMonthlyCost);
        result.put("totalYearlyCost", totalMonthlyCost * 12);
        
        return result;
    }

    /**
     * 전체 Stage 손익계산서 생성
     */
    public Map<String, Object> generateIncomeStatement(
            Map<String, List<Map<String, Object>>> allStageClassifications,
            int budget) {
        
        Map<String, Integer> totalByCategory = new HashMap<>();
        int totalMonthlyCost = 0;
        
        // 모든 Stage의 비용 합산
        for (List<Map<String, Object>> stageClassifications : allStageClassifications.values()) {
            for (Map<String, Object> classification : stageClassifications) {
                String category = (String) classification.get("category");
                Integer monthlyCost = (Integer) classification.get("monthlyCost");
                
                totalByCategory.put(category, totalByCategory.getOrDefault(category, 0) + monthlyCost);
                totalMonthlyCost += monthlyCost;
            }
        }
        
        int totalYearlyCost = totalMonthlyCost * 12;
        int remainingBudget = budget - totalYearlyCost;
        
        Map<String, Object> incomeStatement = new HashMap<>();
        incomeStatement.put("budget", budget);
        incomeStatement.put("totalMonthlyCost", totalMonthlyCost);
        incomeStatement.put("totalYearlyCost", totalYearlyCost);
        incomeStatement.put("remainingBudget", remainingBudget);
        incomeStatement.put("budgetUsageRate", (double) totalYearlyCost / budget * 100);
        incomeStatement.put("categoryBreakdown", totalByCategory);
        
        return incomeStatement;
    }
}
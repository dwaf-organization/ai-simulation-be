package com.example.chatgpt.service;

import com.example.chatgpt.entity.*;
import com.example.chatgpt.repository.*;
import com.example.chatgpt.util.DatabaseLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@Slf4j
@RequiredArgsConstructor
public class StageSummaryGeneratorService {

    private final LlmQuestionRepository llmQuestionRepository;
    private final GroupSummaryRepository groupSummaryRepository;
    private final OperatingExpenseRepository operatingExpenseRepository;
    private final StageSummaryRepository stageSummaryRepository;
    private final Stage1BizplanRepository stage1BizplanRepository;
    private final RevenueModelRepository revenueModelRepository;
    private final OpenAiService openAiService;
    private final DatabaseLockManager lockManager;  // Lock Manager 추가

    /**
     * 스테이지 완료 시 전체 프로세스 실행 (데드락 방지 적용)
     */
    @Transactional
    public Map<String, Object> generateStageSummary(
            Integer eventCode,
            Integer teamCode,
            int stage,
            List<Map<String, Object>> answers) {
        
        log.info("스테이지 {} 요약 생성 시작 - eventCode: {}, teamCode: {}", stage, eventCode, teamCode);
        
        // 데드락 방지를 위한 Lock 적용
        return lockManager.executeWithLock(DatabaseLockManager.ServiceType.STAGE_SUMMARY, () -> {
            try {
                // 1. 답변 데이터를 llm_question 테이블에 업데이트
                updateAnswersToQuestions(teamCode, stage, answers);
                
                // 2. 모든 데이터 수집 (사업계획서, 수익모델, 질문/답변)
                Map<String, Object> allData = collectAllData(eventCode, teamCode, stage);
                
                // 3. Group Summary 생성 및 저장
                GroupSummary groupSummary = generateAndSaveGroupSummary(eventCode, teamCode, stage, allData);
                
                // 4. Operating Expense 생성 및 저장
                List<OperatingExpense> operatingExpenses = generateAndSaveOperatingExpenses(eventCode, teamCode, stage, allData);
                
                // 5. Stage Summary 생성 및 저장
                StageSummary stageSummary = generateAndSaveStageSummary(eventCode, teamCode, stage, groupSummary);
                
                // 6. 결과 응답
                Map<String, Object> result = new HashMap<>();
                result.put("groupSummary", groupSummary);
                result.put("operatingExpenses", operatingExpenses);
                result.put("stageSummary", stageSummary);
                result.put("totalAnswers", answers.size());
                result.put("message", "스테이지 " + stage + " 요약 생성 완료");
                
                log.info("스테이지 {} 요약 생성 완료 - teamCode: {}", stage, teamCode);
                
                return result;
                
            } catch (Exception e) {
                log.error("스테이지 {} 요약 생성 실패 - eventCode: {}, teamCode: {}", stage, eventCode, teamCode, e);
                throw new RuntimeException("스테이지 요약 생성 실패: " + e.getMessage());
            }
        });
    }
    
    /**
     * 1. 답변을 llm_question 테이블에 업데이트
     */
    private void updateAnswersToQuestions(Integer teamCode, int stage, List<Map<String, Object>> answers) {
        log.info("질문 답변 업데이트 시작 - teamCode: {}, stage: {}, 답변 수: {}", teamCode, stage, answers.size());
        
        List<LlmQuestion> questions = llmQuestionRepository.findByTeamCodeAndStageStep(teamCode, stage);
        
        if (questions.size() != answers.size()) {
            throw new RuntimeException(
                String.format("질문 수(%d)와 답변 수(%d)가 일치하지 않습니다.", questions.size(), answers.size())
            );
        }
        
        for (Map<String, Object> answerData : answers) {
            Integer questionId = (Integer) answerData.get("questionId");
            String answer = (String) answerData.get("answer");
            
            // questionId로 질문 찾기
            Optional<LlmQuestion> questionOpt = questions.stream()
                .filter(q -> q.getQuestionCode().equals(questionId))
                .findFirst();
            
            if (questionOpt.isPresent()) {
                LlmQuestion question = questionOpt.get();
                question.setUserAnswer(answer);
                llmQuestionRepository.save(question);
                log.debug("질문 {} 답변 업데이트: {}", questionId, answer.length() > 50 ? answer.substring(0, 50) + "..." : answer);
            } else {
                throw new RuntimeException("질문 ID " + questionId + "를 찾을 수 없습니다.");
            }
        }
        
        log.info("질문 답변 업데이트 완료");
    }
    
    /**
     * 2. 모든 관련 데이터 수집
     */
    private Map<String, Object> collectAllData(Integer eventCode, Integer teamCode, int stage) {
        log.info("데이터 수집 시작 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stage);
        
        Map<String, Object> allData = new HashMap<>();
        
        // 사업계획서 데이터
        Optional<Stage1Bizplan> bizplanOpt = stage1BizplanRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
        if (bizplanOpt.isPresent()) {
            allData.put("bizplan", bizplanOpt.get());
            log.info("사업계획서 데이터 수집 완료");
        } else {
            log.warn("사업계획서 데이터 없음 - eventCode: {}, teamCode: {}", eventCode, teamCode);
        }
        
        // 수익모델 데이터
        Optional<RevenueModel> revenueModelOpt = revenueModelRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
        if (revenueModelOpt.isPresent()) {
            allData.put("revenueModel", revenueModelOpt.get());
            log.info("수익모델 데이터 수집 완료");
        } else {
            log.warn("수익모델 데이터 없음 - eventCode: {}, teamCode: {}", eventCode, teamCode);
        }
        
        // 질문/답변 데이터
        List<LlmQuestion> questions = llmQuestionRepository.findByTeamCodeAndStageStep(teamCode, stage);
        allData.put("questions", questions);
        log.info("질문/답변 데이터 수집 완료 - {} 개 질문", questions.size());
        
        return allData;
    }
    
    /**
     * 3. Group Summary 생성 및 저장
     */
    private GroupSummary generateAndSaveGroupSummary(Integer eventCode, Integer teamCode, int stage, Map<String, Object> allData) {
        log.info("Group Summary 생성 시작");
        
        // 기존 데이터 삭제 (덮어쓰기)
        groupSummaryRepository.deleteByTeamCodeAndEventCodeAndStageStep(teamCode, eventCode, stage);
        
        // ChatGPT 프롬프트 생성
        String prompt = createGroupSummaryPrompt(stage, allData);
        
        // ChatGPT 호출
        String response = openAiService.chat(prompt);
        
        // 응답 파싱 및 저장
        GroupSummary groupSummary = parseAndSaveGroupSummary(eventCode, teamCode, stage, response);
        
        log.info("Group Summary 생성 완료 - summaryId: {}", groupSummary.getSummaryId());
        
        return groupSummary;
    }
    
    /**
     * 4. Operating Expense 생성 및 저장
     */
    private List<OperatingExpense> generateAndSaveOperatingExpenses(Integer eventCode, Integer teamCode, int stage, Map<String, Object> allData) {
        log.info("Operating Expense 생성 시작");
        
        // 기존 데이터 삭제
        operatingExpenseRepository.deleteByTeamCodeAndStageStep(teamCode, stage);
        
        @SuppressWarnings("unchecked")
        List<LlmQuestion> questions = (List<LlmQuestion>) allData.get("questions");
        
        List<OperatingExpense> expenses = new ArrayList<>();
        
        for (LlmQuestion question : questions) {
            OperatingExpense expense = OperatingExpense.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stage)
                .llmResponse(question.getUserAnswer())
                .expenseAmount(null) // 일단 NULL
                .build();
                
            expenses.add(operatingExpenseRepository.save(expense));
        }
        
        log.info("Operating Expense 생성 완료 - {} 개", expenses.size());
        
        return expenses;
    }
    
    /**
     * 5. Stage Summary 생성 및 저장
     */
    private StageSummary generateAndSaveStageSummary(Integer eventCode, Integer teamCode, int stage, GroupSummary groupSummary) {
        log.info("Stage Summary 생성 시작");
        
        // 기존 데이터 삭제
        stageSummaryRepository.deleteByTeamCodeAndEventCodeAndStageStep(teamCode, eventCode, stage);
        
        // Group Summary 기반으로 마크다운 요약 생성
        String summaryPrompt = createStageSummaryPrompt(stage, groupSummary);
        String summaryResponse = openAiService.chat(summaryPrompt);
        
        StageSummary stageSummary = StageSummary.builder()
            .eventCode(eventCode)
            .teamCode(teamCode)
            .stageStep(stage)
            .summaryText(summaryResponse)
            .build();
            
        stageSummary = stageSummaryRepository.save(stageSummary);
        
        log.info("Stage Summary 생성 완료 - summaryCode: {}", stageSummary.getSummaryCode());
        
        return stageSummary;
    }
    
    /**
     * Group Summary용 ChatGPT 프롬프트 생성
     */
    private String createGroupSummaryPrompt(int stage, Map<String, Object> allData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 사업계획서 분석 전문가입니다.\n\n");
        prompt.append(String.format("# Stage %d 종합 분석\n\n", stage));
        
        // 사업계획서 정보
        Stage1Bizplan bizplan = (Stage1Bizplan) allData.get("bizplan");
        if (bizplan != null) {
            prompt.append("# 사업계획서 정보:\n");
            prompt.append(bizplan.getBizItemSummary() != null ? bizplan.getBizItemSummary() : bizplan.getBizplanContent());
            prompt.append("\n\n");
        }
        
        // 수익모델 정보
        RevenueModel revenueModel = (RevenueModel) allData.get("revenueModel");
        if (revenueModel != null) {
            prompt.append("# 수익모델 정보:\n");
            prompt.append("수익 카테고리: ").append(revenueModel.getRevenueCategory()).append("\n");
            // 추가 수익모델 정보 포함 가능
            prompt.append("\n");
        }
        
        // 질문/답변 정보
        @SuppressWarnings("unchecked")
        List<LlmQuestion> questions = (List<LlmQuestion>) allData.get("questions");
        prompt.append("# 질문/답변 정보:\n");
        for (int i = 0; i < questions.size(); i++) {
            LlmQuestion q = questions.get(i);
            prompt.append(String.format("질문 %d: %s\n", i + 1, q.getQuestion()));
            prompt.append(String.format("답변: %s\n\n", q.getUserAnswer()));
        }
        
        prompt.append("# 요청사항:\n");
        prompt.append("위 정보를 종합 분석하여 다음 JSON 형식으로 응답해주세요:\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"business_type\": \"사업 유형 (50자 이내)\",\n");
        prompt.append("  \"core_technology\": \"핵심 기술/역량 (100자 이내)\",\n");
        prompt.append("  \"revenue_model\": \"수익 모델 요약 (100자 이내)\",\n");
        prompt.append("  \"key_answers\": \"주요 답변 요약 (500자 이내)\",\n");
        prompt.append("  \"investment_scale\": \"투자 규모 요약 (50자 이내)\",\n");
        prompt.append("  \"strengths\": \"사업 강점 (300자 이내)\",\n");
        prompt.append("  \"weaknesses\": \"사업 약점/리스크 (300자 이내)\",\n");
        prompt.append("  \"summary_text\": \"ChatGPT 메모리용 압축 요약 (1000자 이내)\"\n");
        prompt.append("}\n\n");
        
        prompt.append("**중요**: JSON 형식으로만 응답하고, 다른 설명은 추가하지 마세요.");
        
        return prompt.toString();
    }
    
    /**
     * Stage Summary용 ChatGPT 프롬프트 생성
     */
    private String createStageSummaryPrompt(int stage, GroupSummary groupSummary) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 사업계획서 요약 전문가입니다.\n\n");
        prompt.append(String.format("# Stage %d 최종 요약 생성\n\n", stage));
        
        prompt.append("# 분석 완료된 정보:\n");
        prompt.append("사업 유형: ").append(groupSummary.getBusinessType()).append("\n");
        prompt.append("핵심 기술: ").append(groupSummary.getCoreTechnology()).append("\n");
        prompt.append("수익 모델: ").append(groupSummary.getRevenueModel()).append("\n");
        prompt.append("투자 규모: ").append(groupSummary.getInvestmentScale()).append("\n");
        prompt.append("사업 강점: ").append(groupSummary.getStrengths()).append("\n");
        prompt.append("사업 약점: ").append(groupSummary.getWeaknesses()).append("\n");
        prompt.append("주요 답변: ").append(groupSummary.getKeyAnswers()).append("\n\n");
        
        prompt.append("# 요청사항:\n");
        prompt.append(String.format("위 정보를 바탕으로 Stage %d에 적합한 **마크다운 형식**의 종합 요약문을 작성해주세요.\n\n", stage));
        
        prompt.append("## 포함 요소:\n");
        prompt.append("1. **## 사업 개요** - 사업 유형과 핵심 기술\n");
        prompt.append("2. **## 수익 모델** - 수익 구조와 투자 규모\n");
        prompt.append("3. **## 강점 분석** - 경쟁 우위 요소\n");
        prompt.append("4. **## 리스크 요인** - 주요 위험 요소와 대응방안\n");
        prompt.append(String.format("5. **## Stage %d 핵심 성과** - 이번 단계의 주요 결과\n\n", stage));
        
        prompt.append("**중요**: \n");
        prompt.append("- 마크다운 형식으로 작성 (##, -, *, ** 등 활용)\n");
        prompt.append("- 읽기 쉽고 전문적인 톤\n");
        prompt.append("- 2000자 이내로 작성\n");
        prompt.append("- 다른 설명 없이 마크다운 텍스트만 응답\n");
        
        return prompt.toString();
    }
    
    /**
     * Group Summary 응답 파싱 및 저장
     */
    private GroupSummary parseAndSaveGroupSummary(Integer eventCode, Integer teamCode, int stage, String response) {
        try {
            // JSON 파싱 (간단한 방법으로 처리)
            String jsonText = response;
            if (response.contains("```json")) {
                jsonText = response.substring(
                    response.indexOf("```json") + 7,
                    response.lastIndexOf("```")
                ).trim();
            } else if (response.contains("```")) {
                jsonText = response.substring(
                    response.indexOf("```") + 3,
                    response.lastIndexOf("```")
                ).trim();
            }
            
            // JSON 수동 파싱 (ObjectMapper 사용 가능하지만 간단하게 처리)
            GroupSummary groupSummary = GroupSummary.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stage)
                .businessType(extractJsonValue(jsonText, "business_type"))
                .coreTechnology(extractJsonValue(jsonText, "core_technology"))
                .revenueModel(extractJsonValue(jsonText, "revenue_model"))
                .keyAnswers(extractJsonValue(jsonText, "key_answers"))
                .investmentScale(extractJsonValue(jsonText, "investment_scale"))
                .strengths(extractJsonValue(jsonText, "strengths"))
                .weaknesses(extractJsonValue(jsonText, "weaknesses"))
                .summaryText(extractJsonValue(jsonText, "summary_text"))
                .build();
                
            return groupSummaryRepository.save(groupSummary);
            
        } catch (Exception e) {
            log.error("Group Summary 파싱 실패: {}", e.getMessage());
            log.error("원본 응답: {}", response);
            throw new RuntimeException("Group Summary 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * JSON에서 값 추출하는 헬퍼 메서드
     */
    private String extractJsonValue(String jsonText, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(jsonText);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}
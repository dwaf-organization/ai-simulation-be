package com.example.chatgpt.service;

import com.example.chatgpt.entity.Event;
import com.example.chatgpt.entity.GroupSummary;
import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.entity.TeamRevenueAllocation;
import com.example.chatgpt.entity.LoanBusinessPlan;
import com.example.chatgpt.repository.EventRepository;
import com.example.chatgpt.repository.GroupSummaryRepository;
import com.example.chatgpt.repository.FinancialStatementRepository;
import com.example.chatgpt.repository.TeamRevenueAllocationRepository;
import com.example.chatgpt.repository.LoanBusinessPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminTriggerService {

    private final EventRepository eventRepository;
    private final GroupSummaryRepository groupSummaryRepository;
    private final FinancialStatementRepository financialStatementRepository;
    private final TeamRevenueAllocationRepository teamRevenueAllocationRepository;
    private final LoanBusinessPlanRepository loanBusinessPlanRepository;
    private final BusinessPlanAnalyzer businessPlanAnalyzer; // ChatGPT API 호출

    /**
     * 관리자 트리거 - 요약보기 일괄처리
     */
    @Transactional
    public String triggerSummaryViewProcess(Integer eventCode, Integer stage) {
        log.info("요약보기 일괄처리 시작 - eventCode: {}, stage: {}", eventCode, stage);
        
        // 1. event.summary_view_process 업데이트 (현재 스테이지 + 1)
        updateSummaryViewProcess(eventCode, stage);
        
        // 2. 해당 행사/스테이지의 모든 팀 조회
        List<GroupSummary> teamSummaries = getTeamSummaries(eventCode, stage);
        
        if (teamSummaries.isEmpty()) {
            throw new RuntimeException("해당 행사/스테이지에 팀 요약 데이터가 없습니다.");
        }
        
        // 3. ChatGPT를 통한 매출 생성
        Map<Integer, RevenueData> teamRevenues = generateTeamRevenues(teamSummaries, eventCode, stage);
        
        // 4. financial_statement 테이블에 매출 저장 (나머지는 0)
        saveFinancialStatements(teamRevenues, eventCode, stage);
        
        // 5. 팀별 순위 생성 및 team_revenue_allocation 저장
        saveTeamRankings(teamRevenues, eventCode, stage);
        
        String result = String.format("총 %d개 팀의 매출 생성 및 순위 산정 완료", teamSummaries.size());
        log.info("요약보기 일괄처리 완료 - {}", result);
        
        return result;
    }
    
    /**
     * Stage1 전용 트리거 - summary_view_process만 업데이트 (매출/지출 처리 없음)
     */
    @Transactional
    public String triggerStage1SummaryView(Integer eventCode) {
        log.info("Stage1 요약보기 처리 시작 - eventCode: {}", eventCode);
        
        // 1. 행사 존재 여부 확인
        Event event = eventRepository.findById(eventCode)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 행사입니다. eventCode: " + eventCode));
        
        // 2. summary_view_process를 2로 설정 (Stage1 완료)
        event.setSummaryViewProcess(1);
        eventRepository.save(event);
        
        log.info("Stage1 요약보기 처리 완료 - eventCode: {}, summary_view_process: 2", eventCode);
        
        return "Stage1 요약보기 처리가 완료되었습니다.";
    }
    
    /**
     * 1. event.summary_view_process 업데이트
     */
    private void updateSummaryViewProcess(Integer eventCode, Integer stage) {
        Event event = eventRepository.findById(eventCode)
            .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다. eventCode: " + eventCode));
        
        event.setSummaryViewProcess(stage);
        eventRepository.save(event);
        
        log.info("summary_view_process 업데이트 완료 - eventCode: {}, 값: {}", eventCode, stage);
    }
    
    /**
     * 관리자 트리거 - stage_batch_process 변경
     */
    @Transactional
    public String updateStageBatchProcess(Integer eventCode, Integer stageStep) {
        try {
            log.info("stage_batch_process 변경 요청 - eventCode: {}, stageStep: {}", eventCode, stageStep);
            
            // 1. 행사 존재 여부 확인
            Event event = eventRepository.findById(eventCode)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 행사입니다. eventCode: " + eventCode));
            
            // 2. stageStep 유효성 검증
            if (stageStep == null || stageStep < 1 || stageStep > 7) {
                throw new RuntimeException("유효하지 않은 스테이지입니다. (1-7 범위)");
            }
            
            Integer oldStage = event.getStageBatchProcess();
            
            // 3. stage_batch_process 업데이트
            event.setStageBatchProcess(stageStep);
            eventRepository.save(event);
            
            log.info("stage_batch_process 변경 완료 - eventCode: {}, {} -> {}", eventCode, oldStage, stageStep);
            
            return String.format("스테이지가 %d에서 %d로 변경되었습니다.", oldStage, stageStep);
            
        } catch (RuntimeException e) {
            log.error("stage_batch_process 변경 실패: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("stage_batch_process 변경 중 시스템 오류", e);
            throw new RuntimeException("스테이지 변경 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 2. 해당 행사/스테이지의 모든 팀 요약 조회
     */
    private List<GroupSummary> getTeamSummaries(Integer eventCode, Integer stage) {
        List<GroupSummary> summaries = groupSummaryRepository.findByEventCodeAndStageStep(eventCode, stage);
        log.info("팀 요약 조회 완료 - {} 개 팀", summaries.size());
        return summaries;
    }
    
    /**
     * 3. ChatGPT를 통한 매출 생성
     */
    private Map<Integer, RevenueData> generateTeamRevenues(List<GroupSummary> teamSummaries, Integer eventCode, Integer stage) {
        log.info("ChatGPT 매출 생성 시작 - {} 개 팀", teamSummaries.size());
        
        // ChatGPT 프롬프트 생성
        String prompt = createRevenueGenerationPrompt(teamSummaries, stage);
        
        try {
            // ChatGPT API 호출
            String response = businessPlanAnalyzer.callChatGptApi(prompt);
            
            // JSON 파싱하여 팀별 매출 데이터 추출
            Map<Integer, RevenueData> teamRevenues = parseRevenueResponse(response, teamSummaries);
            
            log.info("ChatGPT 매출 생성 완료 - {} 개 팀", teamRevenues.size());
            return teamRevenues;
            
        } catch (Exception e) {
            log.error("ChatGPT 매출 생성 실패", e);
            throw new RuntimeException("매출 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * ChatGPT 매출 생성 프롬프트 생성
     */
    private String createRevenueGenerationPrompt(List<GroupSummary> teamSummaries, Integer stage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 스타트업 사업계획 분석 전문가입니다.\n\n");
        prompt.append("# Stage ").append(stage).append(" 팀별 매출 생성\n\n");
        prompt.append("다음 팀들의 사업계획 요약을 분석하여 각 팀에게 적절한 월 매출액을 배정해주세요.\n\n");
        prompt.append("## 매출 생성 기준:\n");
        prompt.append("- 매출 범위: 2천만원 ~ 5억원\n");
        prompt.append("- 팀간 차이: 30% 이내로 유지하되 사업 유망성에 따라 차등 적용\n");
        prompt.append("- 사업 유형, 기술력, 시장성, 경쟁력 등을 종합 고려\n\n");
        prompt.append("## 팀별 정보:\n");
        
        for (int i = 0; i < teamSummaries.size(); i++) {
            GroupSummary summary = teamSummaries.get(i);
            prompt.append(String.format("### 팀 %d (teamCode: %d)\n", i + 1, summary.getTeamCode()));
            prompt.append("- 사업유형: ").append(summary.getBusinessType()).append("\n");
            prompt.append("- 핵심기술: ").append(summary.getCoreTechnology()).append("\n");
            prompt.append("- 수익모델: ").append(summary.getRevenueModel()).append("\n");
            prompt.append("- 투자규모: ").append(summary.getInvestmentScale()).append("\n");
            prompt.append("- 강점: ").append(summary.getStrengths()).append("\n");
            prompt.append("- 약점: ").append(summary.getWeaknesses()).append("\n\n");
        }
        
        prompt.append("## 요청사항:\n");
        prompt.append("위 정보를 바탕으로 각 팀의 예상 월 매출액과 배정 근거를 JSON 형식으로 응답해주세요.\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"teams\": [\n");
        
        for (int i = 0; i < teamSummaries.size(); i++) {
            GroupSummary summary = teamSummaries.get(i);
            prompt.append("    {\n");
            prompt.append("      \"teamCode\": ").append(summary.getTeamCode()).append(",\n");
            prompt.append("      \"revenue\": 예상매출액(숫자만),\n");
            prompt.append("      \"reason\": \"배정 근거 설명\",\n");
            prompt.append("      \"shortReason\": \"요약된 근거\"\n");
            prompt.append("    }");
            if (i < teamSummaries.size() - 1) prompt.append(",");
            prompt.append("\n");
        }
        
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("**중요**: JSON 형식만 응답하고 다른 설명은 포함하지 마세요.");
        
        return prompt.toString();
    }
    
    /**
     * ChatGPT 응답 파싱
     */
    private Map<Integer, RevenueData> parseRevenueResponse(String response, List<GroupSummary> teamSummaries) {
        Map<Integer, RevenueData> result = new HashMap<>();
        
        try {
            // JSON 추출 (마크다운 코드블록 제거)
            String jsonStr = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            // 간단한 JSON 파싱 (정규식 사용)
            String teamsPattern = "\"teams\"\\s*:\\s*\\[(.*?)\\]";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(teamsPattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(jsonStr);
            
            if (matcher.find()) {
                String teamsContent = matcher.group(1);
                
                // 각 팀 데이터 추출
                String teamPattern = "\\{([^}]*)\\}";
                java.util.regex.Pattern teamPat = java.util.regex.Pattern.compile(teamPattern);
                java.util.regex.Matcher teamMatcher = teamPat.matcher(teamsContent);
                
                while (teamMatcher.find()) {
                    String teamData = teamMatcher.group(1);
                    
                    Integer teamCode = extractIntegerValue(teamData, "teamCode");
                    Long revenue = extractLongValue(teamData, "revenue");
                    String reason = extractStringValue(teamData, "reason");
                    String shortReason = extractStringValue(teamData, "shortReason");
                    
                    if (teamCode != null && revenue != null) {
                        result.put(teamCode, new RevenueData(revenue, reason, shortReason));
                    }
                }
            }
            
            // 결과 검증
            if (result.size() != teamSummaries.size()) {
                throw new RuntimeException("ChatGPT 응답 파싱 실패: 팀 수가 일치하지 않습니다.");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("ChatGPT 응답 파싱 실패: {}", response, e);
            throw new RuntimeException("매출 데이터 파싱 실패: " + e.getMessage());
        }
    }
    
    /**
     * 정규식으로 Integer 값 추출
     */
    private Integer extractIntegerValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
    
    /**
     * 정규식으로 Long 값 추출
     */
    private Long extractLongValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }
    
    /**
     * 정규식으로 String 값 추출
     */
    private String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    /**
     * 4. financial_statement 테이블에 매출 저장
     */
    private void saveFinancialStatements(Map<Integer, RevenueData> teamRevenues, Integer eventCode, Integer stage) {
        log.info("financial_statement 저장 시작 - {} 개 팀", teamRevenues.size());
        
        for (Map.Entry<Integer, RevenueData> entry : teamRevenues.entrySet()) {
            Integer teamCode = entry.getKey();
            RevenueData revenueData = entry.getValue();
            
            // 기존 데이터 삭제 (덮어쓰기)
            financialStatementRepository.deleteByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stage);
            
            // loan_business_plan에서 calculated_loan_amount 조회
            Integer accountsPayable = getLoanAmount(eventCode, teamCode, stage);
            
            // 새 financial_statement 생성 (매출 + accounts_payable 설정)
            FinancialStatement fs = FinancialStatement.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stage)
                .revenue(revenueData.getRevenue().intValue()) // 매출 설정
                .accountsPayable(accountsPayable) // 대출금액 설정
                // 나머지는 기본값(null 또는 0)
                .build();
            
            financialStatementRepository.save(fs);
            
            log.debug("팀 {}번 - 매출: {}원, 대출금액: {}원", teamCode, 
                     revenueData.getRevenue(), accountsPayable);
        }
        
        log.info("financial_statement 저장 완료");
    }
    
    /**
     * loan_business_plan에서 calculated_loan_amount 조회
     */
    private Integer getLoanAmount(Integer eventCode, Integer teamCode, Integer stageStep) {
        try {
            Optional<LoanBusinessPlan> loanPlan = loanBusinessPlanRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (loanPlan.isPresent()) {
                Integer calculatedLoanAmount = loanPlan.get().getCalculatedLoanAmount();
                
                // 0이거나 null이면 0 처리
                if (calculatedLoanAmount == null || calculatedLoanAmount == 0) {
                    log.debug("팀 {}번 - 대출금액이 0 또는 null, 0으로 처리", teamCode);
                    return 0;
                }
                
                log.debug("팀 {}번 - 대출금액 조회 성공: {}만원", teamCode, calculatedLoanAmount);
                // 만원 단위를 원 단위로 변환
                return calculatedLoanAmount * 10000;
                
            } else {
                log.debug("팀 {}번 - 대출 사업계획서 없음, 0으로 처리", teamCode);
                return 0;
            }
            
        } catch (Exception e) {
            log.warn("팀 {}번 - 대출금액 조회 실패, 0으로 처리: {}", teamCode, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 5. 팀별 순위 생성 및 저장
     */
    private void saveTeamRankings(Map<Integer, RevenueData> teamRevenues, Integer eventCode, Integer stage) {
        log.info("팀별 순위 생성 시작");
        
        // 매출 기준 순위 정렬
        List<Map.Entry<Integer, RevenueData>> sortedTeams = teamRevenues.entrySet()
            .stream()
            .sorted((e1, e2) -> e2.getValue().getRevenue().compareTo(e1.getValue().getRevenue()))
            .collect(Collectors.toList());
        
        // distribution_id 생성
        String distributionId = eventCode + "-" + stage;
        
        // 기존 데이터 삭제 (덮어쓰기)
        teamRevenueAllocationRepository.deleteByEventCodeAndStageStep(eventCode, stage);
        
        // team_revenue_allocation 저장
        for (int i = 0; i < sortedTeams.size(); i++) {
            Map.Entry<Integer, RevenueData> entry = sortedTeams.get(i);
            Integer teamCode = entry.getKey();
            RevenueData revenueData = entry.getValue();
            int rank = i + 1;
            
            TeamRevenueAllocation allocation = TeamRevenueAllocation.builder()
                .distributionId(distributionId)
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stage)
                .allocatedRevenue(revenueData.getRevenue())
                .stageRank(rank)
                .allocationReason(revenueData.getReason())
                .build();
            
            teamRevenueAllocationRepository.save(allocation);
        }
        
        log.info("팀별 순위 생성 완료 - {} 개 팀", sortedTeams.size());
    }
    
    /**
     * 매출 데이터 내부 클래스
     */
    private static class RevenueData {
        private final Long revenue;
        private final String reason;
        private final String shortReason;
        
        public RevenueData(Long revenue, String reason, String shortReason) {
            this.revenue = revenue;
            this.reason = reason;
            this.shortReason = shortReason;
        }
        
        public Long getRevenue() { return revenue; }
        public String getReason() { return reason; }
        public String getShortReason() { return shortReason; }
    }
}
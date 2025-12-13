package com.example.chatgpt.service;

import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.dto.financialstatement.respDto.AvailableAmountRespDto;
import com.example.chatgpt.dto.financialstatement.respDto.FinancialStatementDto;
import com.example.chatgpt.dto.financialstatement.respDto.FinancialStatementViewRespDto;
import com.example.chatgpt.dto.financialstatement.respDto.TeamFinancialStatementAllRespDto;
import com.example.chatgpt.repository.FinancialStatementRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialStatementService {
    
    private final FinancialStatementRepository financialStatementRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    
    // 초기 현금 (2억원)
    private static final int INITIAL_CASH = 200_000_000;
    
    /**
     * 재무제표 조회 (DTO 변환) - 새로 추가된 메서드
     */
    public FinancialStatementViewRespDto getFinancialStatementView(Integer eventCode, Integer teamCode, Integer stageStep) {
        log.info("재무제표 조회 요청 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep);
        
        try {
            Optional<FinancialStatement> optionalFs = financialStatementRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (optionalFs.isEmpty()) {
                throw new RuntimeException("해당 스테이지의 재무제표를 찾을 수 없습니다.");
            }
            
            FinancialStatement fs = optionalFs.get();
            FinancialStatementViewRespDto result = FinancialStatementViewRespDto.fromEntity(fs);
            
            log.info("재무제표 조회 성공 - fsCode: {}", fs.getFsCode());
            return result;
            
        } catch (Exception e) {
            log.error("재무제표 조회 실패 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep, e);
            throw new RuntimeException("재무제표 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * Stage 완료 시 재무제표 업데이트
     */
    public FinancialStatement updateFinancialStatement(
            Integer teamCode,
            Integer eventCode,
            Integer stageStep,
            String businessPlan,
            Map<String, Object> stageAnswers,
            List<Map<String, Object>> userExpenseInputs) {
        
        try {
            // 1. 이전 스테이지 재무제표 조회
            FinancialStatement prevFS = getPreviousFinancialStatement(teamCode, stageStep);
            
            // 2. 사용자 입력 지출 분석
            Map<String, Integer> expenseAnalysis = analyzeExpenses(stageAnswers, userExpenseInputs);
            
            // 3. ChatGPT로 자산, 매출 추정
            Map<String, Object> estimations = estimateFinancialItems(
                businessPlan, stageAnswers, userExpenseInputs, stageStep);
            
            // 4. 매출 분배 (같은 이벤트 내 팀들과 적절한 편차로)
            Integer revenue = distributeRevenueAmongTeams(eventCode, teamCode, estimations);
            
            // 5. 재무제표 계산 및 생성
            FinancialStatement newFS = calculateFinancialStatement(
                prevFS, expenseAnalysis, estimations, revenue, teamCode, stageStep);
            
            // 6. 저장
            return financialStatementRepository.save(newFS);
            
        } catch (Exception e) {
            log.error("재무제표 업데이트 실패 - teamCode: {}, stage: {}", teamCode, stageStep, e);
            throw new RuntimeException("재무제표 업데이트 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 이전 스테이지 재무제표 조회
     */
    private FinancialStatement getPreviousFinancialStatement(Integer teamCode, Integer stageStep) {
        if (stageStep == 2) {
            // Stage 2는 초기 상태
            return FinancialStatement.builder()
                    .teamCode(teamCode)
                    .stageStep(1)
                    .cashAndDeposits(INITIAL_CASH)
                    .totalAssets(INITIAL_CASH)
                    .capitalStock(INITIAL_CASH)
                    .totalLiabilitiesEquity(INITIAL_CASH)
                    .revenue(0)
                    .sgnaExpenses(0)
                    .rndExpenses(0)
                    .operatingIncome(0)
                    .netIncome(0)
                    .build();
        }
        
        return financialStatementRepository
                .findByTeamCodeAndStageStep(teamCode, stageStep - 1)
                .orElseThrow(() -> new RuntimeException("이전 스테이지 재무제표를 찾을 수 없습니다."));
    }
    
    /**
     * 사용자 입력 지출 분석
     */
    private Map<String, Integer> analyzeExpenses(
            Map<String, Object> stageAnswers, 
            List<Map<String, Object>> userExpenseInputs) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 질문-답변과 사용자 지출 입력을 분석하여 판매비와관리비, 연구개발비로 분류하세요.\n\n");
        
        prompt.append("# 질문-답변:\n");
        prompt.append(formatAnswersForPrompt(stageAnswers));
        
        prompt.append("\n# 사용자 지출 입력:\n");
        for (Map<String, Object> expense : userExpenseInputs) {
            prompt.append("- ").append(expense.get("expenseAmount")).append("\n");
            prompt.append("  (").append(expense.get("expenseDescription")).append(")\n");
        }
        
        prompt.append("\n# 분류 기준:\n");
        prompt.append("- 판매비와관리비: 인건비, 임차료, 마케팅비, 일반관리비 등\n");
        prompt.append("- 연구개발비: 개발인력, 연구장비, 기술개발, R&D 관련 비용\n\n");
        
        prompt.append("# 출력 형식 (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"sgna_monthly\": 월간판매비와관리비(숫자만),\n");
        prompt.append("  \"rnd_monthly\": 월간연구개발비(숫자만),\n");
        prompt.append("  \"reasoning\": \"분류 근거\"\n");
        prompt.append("}\n\n");
        prompt.append("주의: 금액은 만원 단위 숫자로만 입력 (예: 1000 = 1000만원)");
        
        try {
            String response = openAiService.chat(prompt.toString());
            return parseExpenseAnalysis(response);
        } catch (Exception e) {
            log.error("지출 분석 실패", e);
            return getDefaultExpenses();
        }
    }
    
    /**
     * ChatGPT로 자산, 매출 등 추정
     */
    private Map<String, Object> estimateFinancialItems(
            String businessPlan,
            Map<String, Object> stageAnswers,
            List<Map<String, Object>> userExpenseInputs,
            Integer stageStep) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("사업계획서와 질문-답변을 바탕으로 재무제표 항목들을 추정하세요.\n\n");
        
        prompt.append("# 사업계획서 요약:\n");
        prompt.append(businessPlan.length() > 2000 ? businessPlan.substring(0, 2000) + "..." : businessPlan);
        
        prompt.append("\n# Stage ").append(stageStep).append(" 답변:\n");
        prompt.append(formatAnswersForPrompt(stageAnswers));
        
        prompt.append("\n# 추정할 항목들:\n");
        prompt.append("- 월간 예상 매출액\n");
        prompt.append("- 유형자산 (사무용품, 장비 등)\n");
        prompt.append("- 무형자산 (소프트웨어, 특허 등)\n");
        prompt.append("- 재고자산 (필요한 경우)\n");
        prompt.append("- 매입채무\n");
        prompt.append("- 매출원가 비율 (매출 대비 %)\n");
        prompt.append("- 영업외수익 (있는 경우)\n\n");
        
        prompt.append("# 출력 형식 (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"monthly_revenue\": 월간예상매출액(만원단위),\n");
        prompt.append("  \"tangible_assets\": 유형자산(만원단위),\n");
        prompt.append("  \"intangible_assets\": 무형자산(만원단위),\n");
        prompt.append("  \"inventory_assets\": 재고자산(만원단위),\n");
        prompt.append("  \"accounts_payable\": 매입채무(만원단위),\n");
        prompt.append("  \"cogs_ratio\": 매출원가비율(0.1~0.8사이소수),\n");
        prompt.append("  \"non_operating_income\": 영업외수익(만원단위),\n");
        prompt.append("  \"reasoning\": \"추정 근거\"\n");
        prompt.append("}\n\n");
        prompt.append("주의: 현실적이고 보수적으로 추정하세요. 매출원가비율은 업종에 맞게 설정하세요.");
        prompt.append("제조업: 50-70%, 서비스업: 20-40%, IT/소프트웨어: 10-30%, 유통업: 60-80%");
        
        try {
            String response = openAiService.chat(prompt.toString());
            return parseEstimationResult(response);
        } catch (Exception e) {
            log.error("재무항목 추정 실패", e);
            return getDefaultEstimations();
        }
    }
    
    /**
     * 같은 이벤트 내 팀들과 적절한 편차로 매출 분배
     */
    private Integer distributeRevenueAmongTeams(Integer eventCode, Integer teamCode, Map<String, Object> estimations) {
        try {
            // 같은 이벤트의 다른 팀들 매출 조회
            List<FinancialStatement> eventTeams = financialStatementRepository.findByEventCode(eventCode);
            
            if (eventTeams.isEmpty()) {
                // 첫 번째 팀이면 기본값 사용
                return (Integer) estimations.getOrDefault("monthly_revenue", 5000); // 기본 5000만원
            }
            
            // 다른 팀들의 평균 매출 계산
            double avgRevenue = eventTeams.stream()
                    .filter(fs -> fs.getRevenue() != null && fs.getRevenue() > 0)
                    .mapToInt(FinancialStatement::getRevenue)
                    .average()
                    .orElse(5000.0);
            
            // ±30% 범위에서 랜덤 편차 적용 (너무 큰 편차 방지)
            Random random = new Random();
            double variance = 0.7 + (random.nextDouble() * 0.6); // 0.7 ~ 1.3 배
            
            return (int) (avgRevenue * variance);
            
        } catch (Exception e) {
            log.warn("매출 분배 실패, 기본값 사용", e);
            return (Integer) estimations.getOrDefault("monthly_revenue", 5000);
        }
    }
    
    /**
     * 재무제표 계산
     */
    private FinancialStatement calculateFinancialStatement(
            FinancialStatement prevFS,
            Map<String, Integer> expenseAnalysis,
            Map<String, Object> estimations,
            Integer revenue,
            Integer teamCode,
            Integer stageStep) {
        
        // 이전 현금에서 이번 스테이지 지출 차감
        int totalMonthlyExpense = expenseAnalysis.getOrDefault("sgna_monthly", 0) + 
                                 expenseAnalysis.getOrDefault("rnd_monthly", 0);
        int newCash = prevFS.getCashAndDeposits() - totalMonthlyExpense;
        
        // 자산 계산
        int tangibleAssets = (Integer) estimations.getOrDefault("tangible_assets", 1000);
        int intangibleAssets = (Integer) estimations.getOrDefault("intangible_assets", 500);
        int inventoryAssets = (Integer) estimations.getOrDefault("inventory_assets", 0);
        
        int totalAssets = newCash + tangibleAssets + intangibleAssets + inventoryAssets;
        
        // 부채 계산
        int accountsPayable = (Integer) estimations.getOrDefault("accounts_payable", 500);
        int borrowings = prevFS.getBorrowings() != null ? prevFS.getBorrowings() : 0;
        
        // 자본 계산 (자산 - 부채)
        int totalLiabilities = accountsPayable + borrowings;
        int equity = totalAssets - totalLiabilities;
        
        // 손익 계산 (AI 기반)
        double cogsRatio = ((Number) estimations.getOrDefault("cogs_ratio", 0.4)).doubleValue(); // 기본 40%
        int cogs = Math.max(1, (int) (revenue * cogsRatio)); // 최소 1만원 이상
        int grossProfit = revenue - cogs;
        int sgnaExpenses = expenseAnalysis.getOrDefault("sgna_monthly", 0);
        int rndExpenses = expenseAnalysis.getOrDefault("rnd_monthly", 0);
        
        // 영업이익 계산
        int operatingIncome = grossProfit - sgnaExpenses - rndExpenses;
        
        // 영업외수익 (AI 추정값 사용)
        int nonOperatingIncome = Math.max(0, (Integer) estimations.getOrDefault("non_operating_income", 0));
        
        // 법인세 계산 (영업이익 + 영업외수익이 양수일 때만)
        int taxableIncome = operatingIncome + nonOperatingIncome;
        int corporateTax = taxableIncome > 0 ? Math.max(1, (int) (taxableIncome * 0.22)) : 0; // 22% 세율, 최소 1만원
        
        // 순이익 계산
        int netIncome = taxableIncome - corporateTax;
        
        return FinancialStatement.builder()
                .teamCode(teamCode)
                .stageStep(stageStep)
                .cashAndDeposits(newCash)
                .tangibleAssets(tangibleAssets)
                .intangibleAssets(intangibleAssets)
                .inventoryAssets(inventoryAssets)
                .totalAssets(totalAssets)
                .accountsPayable(accountsPayable)
                .borrowings(borrowings)
                .capitalStock(prevFS.getCapitalStock()) // 자본금은 유지
                .totalLiabilitiesEquity(totalAssets)
                .revenue(revenue)
                .cogs(cogs)
                .grossProfit(grossProfit)
                .sgnaExpenses(sgnaExpenses)
                .rndExpenses(rndExpenses)
                .operatingIncome(operatingIncome)
                .nonOperatingIncome(nonOperatingIncome)
                .corporateTax(corporateTax)
                .netIncome(netIncome)
                .fsScore(calculateFSScore(revenue, netIncome, totalAssets))
                .build();
    }
    
    /**
     * 재무 점수 계산 (간단한 로직)
     */
    private Integer calculateFSScore(Integer revenue, Integer netIncome, Integer totalAssets) {
        if (revenue == 0 || totalAssets == 0) return 0;
        
        double profitMargin = (double) netIncome / revenue * 100;
        double roa = (double) netIncome / totalAssets * 100;
        
        int score = (int) ((profitMargin * 0.6) + (roa * 0.4));
        return Math.max(0, Math.min(100, score)); // 0-100 범위로 제한
    }
    
    // Helper 메서드들
    private String formatAnswersForPrompt(Map<String, Object> stageAnswers) {
        StringBuilder sb = new StringBuilder();
        stageAnswers.forEach((question, answer) -> {
            sb.append("Q: ").append(question).append("\n");
            sb.append("A: ").append(answer).append("\n\n");
        });
        return sb.toString();
    }
    
    private Map<String, Integer> parseExpenseAnalysis(String response) {
        try {
            String jsonStr = extractJsonFromResponse(response);
            Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            
            Map<String, Integer> expenses = new HashMap<>();
            expenses.put("sgna_monthly", getIntValue(result, "sgna_monthly"));
            expenses.put("rnd_monthly", getIntValue(result, "rnd_monthly"));
            
            return expenses;
        } catch (Exception e) {
            log.error("지출 분석 파싱 실패", e);
            return getDefaultExpenses();
        }
    }
    
    private Map<String, Object> parseEstimationResult(String response) {
        try {
            String jsonStr = extractJsonFromResponse(response);
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("추정 결과 파싱 실패", e);
            return getDefaultEstimations();
        }
    }
    
    private String extractJsonFromResponse(String response) {
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
        return jsonStr.trim();
    }
    
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private Map<String, Integer> getDefaultExpenses() {
        Map<String, Integer> defaults = new HashMap<>();
        defaults.put("sgna_monthly", 1000); // 기본 1000만원
        defaults.put("rnd_monthly", 500);   // 기본 500만원
        return defaults;
    }
    
    private Map<String, Object> getDefaultEstimations() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("monthly_revenue", 3000); // 최소 3000만원
        defaults.put("tangible_assets", 800);  // 최소 800만원
        defaults.put("intangible_assets", 400); // 최소 400만원
        defaults.put("inventory_assets", 200);  // 최소 200만원 (0이 아닌 값)
        defaults.put("accounts_payable", 300);  // 최소 300만원
        defaults.put("cogs_ratio", 0.35);       // 기본 35% (서비스업 수준)
        defaults.put("non_operating_income", 50); // 최소 50만원 영업외수익
        return defaults;
    }
    
    /**
     * 팀의 모든 재무제표 조회
     */
    public List<FinancialStatement> getTeamFinancialStatements(Integer teamCode) {
        return financialStatementRepository.findByTeamCodeOrderByStageStep(teamCode);
    }
    
    /**
     * 특정 스테이지 재무제표 조회
     */
    public Optional<FinancialStatement> getFinancialStatement(Integer teamCode, Integer stageStep) {
        return financialStatementRepository.findByTeamCodeAndStageStep(teamCode, stageStep);
    }
    
    /**
     * 팀별 모든 스테이지 재무제표 조회 (Stage2~7)
     */
    public TeamFinancialStatementAllRespDto getTeamFinancialStatementsAll(Integer eventCode, Integer teamCode) {
        try {
            log.info("팀별 모든 스테이지 재무제표 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀의 모든 재무제표 조회
            List<FinancialStatement> allStatements = financialStatementRepository.findByTeamCodeOrderByStageStep(teamCode);
            
            // 2. 스테이지별로 그룹화 (Stage2~7만)
            Map<Integer, FinancialStatementDto> stageMap = allStatements.stream()
                .filter(fs -> fs.getStageStep() != null && fs.getStageStep() >= 2 && fs.getStageStep() <= 7)
                .filter(fs -> fs.getEventCode().equals(eventCode))  // 이벤트 코드 확인
                .collect(Collectors.toMap(
                    FinancialStatement::getStageStep,
                    FinancialStatementDto::from
                ));
            
            // 3. 응답 DTO 구성 (없는 스테이지는 null)
            TeamFinancialStatementAllRespDto result = TeamFinancialStatementAllRespDto.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stage2(stageMap.get(2))
                .stage3(stageMap.get(3))
                .stage4(stageMap.get(4))
                .stage5(stageMap.get(5))
                .stage6(stageMap.get(6))
                .stage7(stageMap.get(7))
                .build();
            
            // 4. 조회된 스테이지 개수 로깅
            long foundStages = stageMap.size();
            log.info("팀별 재무제표 조회 완료 - eventCode: {}, teamCode: {}, 조회된 스테이지 수: {}개", 
                     eventCode, teamCode, foundStages);
            
            return result;
            
        } catch (Exception e) {
            log.error("팀별 모든 스테이지 재무제표 조회 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            throw new RuntimeException("팀별 재무제표 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 사용 가능한 금액 조회 (스테이지 2부터)
     * 전 스테이지 cash_and_deposits + 현재 스테이지 accounts_payable
     */
    public AvailableAmountRespDto getAvailableAmount(Integer eventCode, Integer teamCode, Integer stageStep) {
        log.info("사용 가능한 금액 조회 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep);
        
        try {
            // 스테이지 2 미만은 지원하지 않음
            if (stageStep < 2) {
                throw new RuntimeException("스테이지 2부터 사용 가능한 금액을 조회할 수 있습니다.");
            }
            
            Integer previousStage = stageStep - 1;
            Integer previousCashAndDeposits = null;
            Integer currentAccountsPayable = null;
            
            // 1. 전 스테이지의 cash_and_deposits 조회
            Optional<FinancialStatement> previousFs = financialStatementRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, previousStage);
            
            if (previousFs.isPresent()) {
                previousCashAndDeposits = previousFs.get().getCashAndDeposits();
                log.info("전 스테이지({}) cash_and_deposits: {}", previousStage, previousCashAndDeposits);
            } else {
                log.warn("전 스테이지({}) 재무제표를 찾을 수 없습니다", previousStage);
                throw new RuntimeException("전 스테이지(" + previousStage + ") 재무제표를 찾을 수 없습니다.");
            }
            
            // 2. 현재 스테이지의 accounts_payable 조회
            Optional<FinancialStatement> currentFs = financialStatementRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (currentFs.isPresent()) {
                currentAccountsPayable = currentFs.get().getAccountsPayable();
                log.info("현재 스테이지({}) accounts_payable: {}", stageStep, currentAccountsPayable);
            } else {
                log.warn("현재 스테이지({}) 재무제표를 찾을 수 없습니다", stageStep);
                throw new RuntimeException("현재 스테이지(" + stageStep + ") 재무제표를 찾을 수 없습니다.");
            }
            
            // 3. DTO 생성 및 반환
            AvailableAmountRespDto result = AvailableAmountRespDto.of(
                eventCode, teamCode, stageStep, 
                previousCashAndDeposits, currentAccountsPayable
            );
            
            log.info("사용 가능한 금액 계산 완료 - 전 스테이지 현금: {}, 현재 스테이지 매입채무: {}, 사용가능금액: {}", 
                     previousCashAndDeposits, currentAccountsPayable, result.getAvailableAmount());
            
            return result;
            
        } catch (RuntimeException e) {
            log.error("사용 가능한 금액 조회 중 비즈니스 오류: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("사용 가능한 금액 조회 중 시스템 오류", e);
            throw new RuntimeException("사용 가능한 금액 조회 중 시스템 오류가 발생했습니다.");
        }
    }
    
}
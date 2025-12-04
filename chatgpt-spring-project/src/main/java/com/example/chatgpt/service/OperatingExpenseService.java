package com.example.chatgpt.service;

import com.example.chatgpt.entity.OperatingExpense;
import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.entity.CompanyCapabilityScore;
import com.example.chatgpt.repository.OperatingExpenseRepository;
import com.example.chatgpt.repository.FinancialStatementRepository;
import com.example.chatgpt.repository.CompanyCapabilityScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperatingExpenseService {

    private final OperatingExpenseRepository operatingExpenseRepository;
    private final FinancialStatementRepository financialStatementRepository;
    private final CompanyCapabilityScoreRepository companyCapabilityScoreRepository;
    private final BusinessPlanAnalyzer businessPlanAnalyzer; // ChatGPT API 호출
    
    /**
     * 지출 조회
     */
    public List<Map<String, Object>> getExpenses(Integer eventCode, Integer teamCode, Integer stage) {
        log.info("지출 조회 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stage);
        
        List<OperatingExpense> expenses = operatingExpenseRepository.findByTeamCodeAndStageStep(teamCode, stage);
        
        if (expenses.isEmpty()) {
            throw new RuntimeException("해당 스테이지의 지출 데이터를 찾을 수 없습니다.");
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (OperatingExpense expense : expenses) {
            if (expense.getLlmResponse() == null || expense.getLlmResponse().trim().isEmpty()) {
                log.warn("빈 답변 발견 - expenseCode: {}", expense.getExpenseCode());
                continue;
            }
            
            Map<String, Object> expenseData = new HashMap<>();
            expenseData.put("expenseCode", expense.getExpenseCode());
            expenseData.put("llmResponse", expense.getLlmResponse());
            expenseData.put("expenseAmount", expense.getExpenseAmount());
            
            result.add(expenseData);
        }
        
        if (result.isEmpty()) {
            throw new RuntimeException("유효한 답변 데이터가 없습니다.");
        }
        
        log.info("지출 조회 완료 - {} 개 항목", result.size());
        
        return result;
    }

    /**
     * 지출 업데이트 + 재무제표 완성
     */
    @Transactional
    public void updateExpenses(List<Map<String, Object>> expenses) {
        log.info("지출 업데이트 시작 - {} 개 항목", expenses.size());
        
        // 1. 지출 금액 업데이트
        Integer eventCode = null;
        Integer teamCode = null;
        Integer stageStep = null;
        
        for (Map<String, Object> expenseData : expenses) {
            Integer expenseCode = (Integer) expenseData.get("expenseCode");
            String expenseAmount = (String) expenseData.get("expenseAmount");
            
            if (expenseCode == null) {
                log.warn("expenseCode가 null입니다. 건너뜁니다.");
                continue;
            }
            
            if (expenseAmount == null || expenseAmount.trim().isEmpty()) {
                log.warn("expenseAmount가 비어있습니다. expenseCode: {}", expenseCode);
                continue;
            }
            
            // 해당 지출 항목 조회 및 업데이트
            Optional<OperatingExpense> expenseOpt = operatingExpenseRepository.findById(expenseCode);
            
            if (expenseOpt.isPresent()) {
                OperatingExpense expense = expenseOpt.get();
                expense.setExpenseAmount(expenseAmount);
                operatingExpenseRepository.save(expense);
                
                // 첫 번째 지출에서 이벤트/팀/스테이지 정보 추출
                if (eventCode == null) {
                    eventCode = expense.getEventCode();
                    teamCode = expense.getTeamCode();
                    stageStep = expense.getStageStep();
                }
                
                log.debug("지출 업데이트 완료 - expenseCode: {}, amount: {}", expenseCode, expenseAmount);
            } else {
                log.warn("지출 항목을 찾을 수 없습니다. expenseCode: {}", expenseCode);
                throw new RuntimeException("지출 항목을 찾을 수 없습니다. expenseCode: " + expenseCode);
            }
        }
        
        log.info("지출 업데이트 완료");
        
        // 2. 재무제표 완성 (지출 업데이트 후 자동 실행)
        if (eventCode != null && teamCode != null && stageStep != null) {
            completeFinancialStatement(eventCode, teamCode, stageStep);
            
            // 3. ✅ 역량 업데이트 (재무제표 완성 후 실행)
            updateTeamCapability(eventCode, teamCode, stageStep, expenses);
        } else {
            log.warn("재무제표 완성을 위한 정보가 부족합니다.");
        }
    }
    
    /**
     * 재무제표 완성
     * TODO: 2차 구현 - 지출 기반 재무제표 자동 생성
     */
    private void completeFinancialStatement(Integer eventCode, Integer teamCode, Integer stageStep) {
        try {
            log.info("재무제표 완성 시작 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stageStep);
            
            // 1. 기존 financial_statement 조회 (매출이 이미 있는지 확인)
            Optional<FinancialStatement> existingFs = financialStatementRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (existingFs.isEmpty()) {
                log.warn("매출이 생성되지 않았습니다. 관리자 트리거를 먼저 실행해주세요.");
                return;
            }
            
            FinancialStatement fs = existingFs.get();
            
            // 2. operating_expense에서 지출 데이터 조회
            List<OperatingExpense> expenses = operatingExpenseRepository.findByTeamCodeAndStageStep(teamCode, stageStep);
            
            if (expenses.isEmpty()) {
                log.warn("지출 데이터가 없습니다.");
                return;
            }
            
            // 3. ChatGPT로 지출 분류 및 재무제표 완성
            Map<String, Long> financialData = classifyExpensesAndGenerateFinancials(fs.getRevenue(), expenses);
            
            // 4. financial_statement 업데이트
            updateFinancialStatement(fs, financialData);
            
            log.info("재무제표 완성 완료 - teamCode: {}", teamCode);
            
        } catch (Exception e) {
            log.error("재무제표 완성 중 오류 발생", e);
            // 지출 업데이트는 이미 완료되었으므로 예외를 던지지 않음
        }
    }
    
    /**
     * ChatGPT로 지출 분류 및 재무제표 생성
     */
    private Map<String, Long> classifyExpensesAndGenerateFinancials(Integer revenue, List<OperatingExpense> expenses) {
        // 지출 정보 정리 및 총 지출액 계산
        StringBuilder expenseInfo = new StringBuilder();
        long totalExpenseAmount = 0;
        
        for (OperatingExpense expense : expenses) {
            if (expense.getExpenseAmount() != null && !expense.getExpenseAmount().trim().isEmpty()) {
                try {
                    long amount = Long.parseLong(expense.getExpenseAmount().replaceAll("[^0-9]", ""));
                    totalExpenseAmount += amount;
                    
                    expenseInfo.append("- ").append(expense.getLlmResponse())
                              .append(" : ").append(amount).append("원\n");
                } catch (NumberFormatException e) {
                    log.warn("지출 금액 파싱 실패: {}", expense.getExpenseAmount());
                }
            }
        }
        
        // 현금 계산: 초기 2억원 - 총 지출
        long initialCash = 200_000_000L; // 2억원
        long remainingCash = initialCash - totalExpenseAmount;
        
        // ChatGPT 프롬프트 생성
        String prompt = createFinancialAnalysisPrompt(revenue, totalExpenseAmount, expenseInfo.toString(), remainingCash);
        
        try {
            // ChatGPT API 호출
            String response = businessPlanAnalyzer.callChatGptApi(prompt);
            
            // 응답 파싱
            Map<String, Long> result = parseFinancialResponse(response);
            
            // 계산된 현금 값 설정 (ChatGPT 결과 덮어쓰기)
            result.put("cashAndDeposits", remainingCash);
            
            return result;
            
        } catch (Exception e) {
            log.error("ChatGPT 재무분석 실패", e);
            // 기본값으로 폴백
            return createDefaultFinancials(revenue, totalExpenseAmount, remainingCash);
        }
    }
    
    /**
     * 재무분석 ChatGPT 프롬프트 생성
     */
    private String createFinancialAnalysisPrompt(Integer revenue, long totalExpenseAmount, String expenseDetails, long remainingCash) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 재무제표 작성 전문가입니다.\n\n");
        prompt.append("# 재무제표 완성 요청\n\n");
        prompt.append("## 기본 정보\n");
        prompt.append("- 월 매출: ").append(String.format("%,d", revenue)).append("원 (기존값 유지)\n");
        prompt.append("- 총 지출: ").append(String.format("%,d", totalExpenseAmount)).append("원\n");
        prompt.append("- 계산된 현금: ").append(String.format("%,d", remainingCash)).append("원 (2억 - 총지출)\n\n");
        
        prompt.append("## 지출 내역 (판관비/R&D 분류 필요)\n");
        prompt.append(expenseDetails).append("\n");
        
        prompt.append("## 요청사항\n");
        prompt.append("1. **중요**: 각 지출을 내용 분석하여 **판매관리비** 또는 **연구개발비**로 분류\n");
        prompt.append("2. 판관비(sgna_expenses)와 R&D(rnd_expenses)는 실제 지출 분류 결과의 합계로 설정\n");
        prompt.append("3. 나머지 재무제표 항목들을 매출 대비 합리적인 비율로 생성\n");
        prompt.append("4. fs_score는 재무상태를 종합적으로 판단하여 1-100점 부여\n\n");
        
        prompt.append("## 분류 기준\n");
        prompt.append("- **판매관리비**: 마케팅, 광고, 인사, 총무, 영업, 관리 관련 비용\n");
        prompt.append("- **연구개발비**: 기술개발, 연구, 특허, 시제품, 개발인력 관련 비용\n\n");
        
        prompt.append("## 응답 형식\n");
        prompt.append("다음 JSON 형식으로 응답해주세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"sgnaExpenses\": 판매관리비_합계(숫자만),\n");
        prompt.append("  \"rndExpenses\": 연구개발비_합계(숫자만),\n");
        prompt.append("  \"cogs\": 매출원가_금액(숫자만),\n");
        prompt.append("  \"grossProfit\": 매출총이익_금액(숫자만),\n");
        prompt.append("  \"operatingIncome\": 영업이익_금액(숫자만),\n");
        prompt.append("  \"nonOperatingIncome\": 영업외수익_금액(숫자만),\n");
        prompt.append("  \"corporateTax\": 법인세_금액(숫자만),\n");
        prompt.append("  \"netIncome\": 순이익_금액(숫자만),\n");
        prompt.append("  \"tangibleAssets\": 유형자산_금액(숫자만),\n");
        prompt.append("  \"inventoryAssets\": 재고자산_금액(숫자만),\n");
        prompt.append("  \"ppeAssets\": 유형자산PPE_금액(숫자만),\n");
        prompt.append("  \"intangibleAssets\": 무형자산_금액(숫자만),\n");
        prompt.append("  \"totalAssets\": 자산총계_금액(숫자만),\n");
//        prompt.append("  \"accountsPayable\": 매입채무_금액(숫자만),\n");
        prompt.append("  \"borrowings\": 차입금_금액(숫자만),\n");
        prompt.append("  \"capitalStock\": 자본금_금액(숫자만),\n");
        prompt.append("  \"totalLiabilitiesEquity\": 부채자본총계_금액(숫자만),\n");
        prompt.append("  \"fsScore\": 재무상태점수(1-100점),\n");
        prompt.append("  \"expenseClassification\": {\n");
        prompt.append("    \"sgnaItems\": [\"판매관리비로 분류된 항목들\"],\n");
        prompt.append("    \"rndItems\": [\"연구개발비로 분류된 항목들\"]\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("**중요**: \n");
        prompt.append("- JSON 형식만 응답하고 다른 설명은 포함하지 마세요\n");
        prompt.append("- revenue와 cashAndDeposits는 생성하지 마세요 (이미 계산됨)\n");
        prompt.append("- sgnaExpenses + rndExpenses = 총 지출액이 되어야 합니다");
        
        return prompt.toString();
    }
    
    /**
     * ChatGPT 재무분석 응답 파싱
     */
    private Map<String, Long> parseFinancialResponse(String response) {
        Map<String, Long> result = new HashMap<>();
        
        try {
            // JSON 추출 (마크다운 코드블록 제거)
            String jsonStr = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            // 각 항목별로 숫자 추출
            result.put("sgnaExpenses", extractLongValue(jsonStr, "sgnaExpenses"));
            result.put("rndExpenses", extractLongValue(jsonStr, "rndExpenses"));
            result.put("cogs", extractLongValue(jsonStr, "cogs"));
            result.put("grossProfit", extractLongValue(jsonStr, "grossProfit"));
            result.put("operatingIncome", extractLongValue(jsonStr, "operatingIncome"));
            result.put("nonOperatingIncome", extractLongValue(jsonStr, "nonOperatingIncome"));
            result.put("corporateTax", extractLongValue(jsonStr, "corporateTax"));
            result.put("netIncome", extractLongValue(jsonStr, "netIncome"));
            
            // 자산 관련
            result.put("tangibleAssets", extractLongValue(jsonStr, "tangibleAssets"));
            result.put("inventoryAssets", extractLongValue(jsonStr, "inventoryAssets"));
            result.put("ppeAssets", extractLongValue(jsonStr, "ppeAssets"));
            result.put("intangibleAssets", extractLongValue(jsonStr, "intangibleAssets"));
            result.put("totalAssets", extractLongValue(jsonStr, "totalAssets"));
            
            // 부채 및 자본
//            result.put("accountsPayable", extractLongValue(jsonStr, "accountsPayable"));
            result.put("borrowings", extractLongValue(jsonStr, "borrowings"));
            result.put("capitalStock", extractLongValue(jsonStr, "capitalStock"));
            result.put("totalLiabilitiesEquity", extractLongValue(jsonStr, "totalLiabilitiesEquity"));
            
            // 점수
            result.put("fsScore", extractLongValue(jsonStr, "fsScore"));
            
            // cashAndDeposits는 별도 계산으로 설정 (여기서는 0으로 임시 설정)
            result.put("cashAndDeposits", 0L);
            
            return result;
            
        } catch (Exception e) {
            log.error("ChatGPT 재무분석 응답 파싱 실패: {}", response, e);
            throw new RuntimeException("재무분석 데이터 파싱 실패");
        }
    }
    
    /**
     * 정규식으로 Long 값 추출
     */
    private Long extractLongValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }
    
    /**
     * 기본 재무제표 데이터 생성 (ChatGPT 실패 시)
     */
    private Map<String, Long> createDefaultFinancials(Integer revenue, long totalExpenseAmount, long remainingCash) {
        Map<String, Long> result = new HashMap<>();
        
        long revenueL = revenue.longValue();
        
        // 지출 분류 (기본값으로 50:50 분할)
        result.put("sgnaExpenses", totalExpenseAmount / 2); // 지출의 50%
        result.put("rndExpenses", totalExpenseAmount / 2);  // 지출의 50%
        
        // 손익계산서
        result.put("cogs", revenueL * 30 / 100);           // 매출의 30%
        result.put("grossProfit", revenueL * 70 / 100);    // 매출의 70%
        result.put("operatingIncome", revenueL * 20 / 100); // 매출의 20%
        result.put("nonOperatingIncome", revenueL * 2 / 100); // 매출의 2%
        result.put("corporateTax", revenueL * 5 / 100);    // 매출의 5%
        result.put("netIncome", revenueL * 15 / 100);      // 매출의 15%
        
        // 자산
        result.put("cashAndDeposits", remainingCash);      // 계산된 현금
        result.put("tangibleAssets", revenueL * 30 / 100); // 매출의 30%
        result.put("inventoryAssets", revenueL * 10 / 100); // 매출의 10%
        result.put("ppeAssets", revenueL * 25 / 100);      // 매출의 25%
        result.put("intangibleAssets", revenueL * 20 / 100); // 매출의 20%
        result.put("totalAssets", revenueL);               // 매출과 동일
        
        // 부채 및 자본
//        result.put("accountsPayable", revenueL * 10 / 100); // 매출의 10%
        result.put("borrowings", revenueL * 10 / 100);        // 매출의 10%
        result.put("capitalStock", revenueL * 50 / 100);   // 매출의 50%
        result.put("totalLiabilitiesEquity", revenueL);    // 매출과 동일
        
        // 점수 (기본값)
        result.put("fsScore", 75L);                        // 기본 75점
        
        log.info("기본 재무제표 데이터 적용 - 현금: {}원", remainingCash);
        return result;
    }
    
    /**
     * financial_statement 업데이트
     */
    private void updateFinancialStatement(FinancialStatement fs, Map<String, Long> financialData) {
        // 손익계산서 (revenue는 기존값 유지)
        fs.setSgnaExpenses(financialData.get("sgnaExpenses").intValue());
        fs.setRndExpenses(financialData.get("rndExpenses").intValue());
        fs.setCogs(financialData.get("cogs").intValue());
        fs.setGrossProfit(financialData.get("grossProfit").intValue());
        fs.setOperatingIncome(financialData.get("operatingIncome").intValue());
        fs.setNonOperatingIncome(financialData.get("nonOperatingIncome").intValue());
        fs.setCorporateTax(financialData.get("corporateTax").intValue());
        fs.setNetIncome(financialData.get("netIncome").intValue());
        
        // 자산
        fs.setCashAndDeposits(financialData.get("cashAndDeposits").intValue());
        fs.setTangibleAssets(financialData.get("tangibleAssets").intValue());
        fs.setInventoryAssets(financialData.get("inventoryAssets").intValue());
        fs.setPpeAssets(financialData.get("ppeAssets").intValue());
        fs.setIntangibleAssets(financialData.get("intangibleAssets").intValue());
        fs.setTotalAssets(financialData.get("totalAssets").intValue());
        
        // 부채 및 자본
        fs.setAccountsPayable(financialData.get("accountsPayable").intValue());
        fs.setBorrowings(financialData.get("borrowings").intValue());
        fs.setCapitalStock(financialData.get("capitalStock").intValue());
        fs.setTotalLiabilitiesEquity(financialData.get("totalLiabilitiesEquity").intValue());
        
        // 점수
        fs.setFsScore(financialData.get("fsScore").intValue());
        
        financialStatementRepository.save(fs);
        
        log.info("재무제표 업데이트 완료 - teamCode: {}, 현금: {}원, 판관비: {}원, R&D: {}원", 
                 fs.getTeamCode(), 
                 fs.getCashAndDeposits(),
                 fs.getSgnaExpenses(),
                 fs.getRndExpenses());
    }
    
    /**
     * 팀 역량 업데이트
     */
    private void updateTeamCapability(Integer eventCode, Integer teamCode, Integer stageStep, 
                                    List<Map<String, Object>> expenses) {
        try {
            log.info("팀 역량 업데이트 시작 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stageStep);
            
            // 1. 기존 역량 점수 조회
            Optional<CompanyCapabilityScore> optionalCapability = 
                companyCapabilityScoreRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (optionalCapability.isEmpty()) {
                log.warn("역량 점수 데이터를 찾을 수 없습니다. - eventCode: {}, teamCode: {}", eventCode, teamCode);
                return;
            }
            
            CompanyCapabilityScore capability = optionalCapability.get();
            
            // 2. 재무제표 조회 (역량 분석용)
            Optional<FinancialStatement> optionalFs = financialStatementRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (optionalFs.isEmpty()) {
                log.warn("재무제표 데이터를 찾을 수 없습니다.");
                return;
            }
            
            FinancialStatement fs = optionalFs.get();
            
            // 3. 지출 내역 정리
            StringBuilder expenseDetails = new StringBuilder();
            for (Map<String, Object> expenseData : expenses) {
                Integer expenseCode = (Integer) expenseData.get("expenseCode");
                String expenseAmount = (String) expenseData.get("expenseAmount");
                
                Optional<OperatingExpense> expenseOpt = operatingExpenseRepository.findById(expenseCode);
                if (expenseOpt.isPresent()) {
                    OperatingExpense expense = expenseOpt.get();
                    expenseDetails.append("- ").append(expense.getLlmResponse())
                                 .append(" : ").append(expenseAmount).append("원\n");
                }
            }
            
            // 4. ChatGPT로 역량 분석 및 업데이트
            Map<String, Integer> capabilityUpdates = analyzeCapabilityWithChatGPT(fs, expenseDetails.toString());
            
            // 5. 역량 점수 업데이트
            for (Map.Entry<String, Integer> entry : capabilityUpdates.entrySet()) {
                capability.addCapabilityScore(entry.getKey(), entry.getValue());
            }
            
            // 6. 저장
            companyCapabilityScoreRepository.save(capability);
            
            log.info("팀 역량 업데이트 완료 - teamCode: {}, 총 역량: {}", teamCode, capability.getTotalCapabilityLevel());
            
        } catch (Exception e) {
            log.error("팀 역량 업데이트 실패", e);
            // 역량 업데이트 실패가 전체 프로세스를 중단시키지 않음
        }
    }
    
    /**
     * ChatGPT로 역량 분석
     */
    private Map<String, Integer> analyzeCapabilityWithChatGPT(FinancialStatement fs, String expenseDetails) {
        // ChatGPT 프롬프트 생성
        String prompt = createCapabilityAnalysisPrompt(fs, expenseDetails);
        
        try {
            // ChatGPT API 호출
            String response = businessPlanAnalyzer.callChatGptApi(prompt);
            
            // 응답 파싱
            return parseCapabilityResponse(response);
            
        } catch (Exception e) {
            log.error("ChatGPT 역량 분석 실패", e);
            // 기본값으로 폴백
            return createDefaultCapabilityUpdates();
        }
    }
    
    /**
     * 역량 분석 ChatGPT 프롬프트 생성
     */
    private String createCapabilityAnalysisPrompt(FinancialStatement fs, String expenseDetails) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 창업팀 역량 분석 전문가입니다.\n\n");
        prompt.append("# 팀 역량 업데이트 분석\n\n");
        prompt.append("## 재무 성과\n");
        prompt.append("- 매출: ").append(String.format("%,d", fs.getRevenue())).append("원\n");
        prompt.append("- 영업이익: ").append(String.format("%,d", fs.getOperatingIncome())).append("원\n");
        prompt.append("- 순이익: ").append(String.format("%,d", fs.getNetIncome())).append("원\n");
        prompt.append("- 현금: ").append(String.format("%,d", fs.getCashAndDeposits())).append("원\n");
        prompt.append("- 판관비: ").append(String.format("%,d", fs.getSgnaExpenses())).append("원\n");
        prompt.append("- R&D비: ").append(String.format("%,d", fs.getRndExpenses())).append("원\n\n");
        
        prompt.append("## 지출 내역\n");
        prompt.append(expenseDetails).append("\n");
        
        prompt.append("## 요청사항\n");
        prompt.append("위 재무 성과와 지출 내역을 분석하여 다음 6개 역량 중 **2개만 선정**하고 각각 1-3점을 부여하세요.\n\n");
        prompt.append("**역량 종류**:\n");
        prompt.append("- strategy: 전략역량 (사업전략, 시장분석, 경쟁전략)\n");
        prompt.append("- finance: 재무역량 (자금관리, 투자, 재무계획)\n");
        prompt.append("- market_customer: 시장고객역량 (고객확보, 마케팅, 영업)\n");
        prompt.append("- operation_management: 운영관리역량 (생산, 공급망, 품질관리)\n");
        prompt.append("- technology_innovation: 기술혁신역량 (R&D, 특허, 기술개발)\n");
        prompt.append("- sustainability: 지속가능성역량 (ESG, 환경, 사회적 가치)\n\n");
        
        prompt.append("**점수 기준**:\n");
        prompt.append("- 1점: 보통 수준의 성과\n");
        prompt.append("- 2점: 좋은 성과\n");
        prompt.append("- 3점: 매우 우수한 성과\n\n");
        
        prompt.append("## 응답 형식\n");
        prompt.append("다음 JSON 형식으로 응답하세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"capability1\": \"역량명(위 6개 중 하나)\",\n");
        prompt.append("  \"score1\": 점수(1-3),\n");
        prompt.append("  \"reason1\": \"점수 부여 근거\",\n");
        prompt.append("  \"capability2\": \"역량명(위 6개 중 하나)\",\n");
        prompt.append("  \"score2\": 점수(1-3),\n");
        prompt.append("  \"reason2\": \"점수 부여 근거\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("**중요**: 정확히 2개 역량만 선정하고, JSON 형식만 응답하세요.");
        
        return prompt.toString();
    }
    
    /**
     * ChatGPT 역량 분석 응답 파싱
     */
    private Map<String, Integer> parseCapabilityResponse(String response) {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            // JSON 추출
            String jsonStr = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            // capability1, score1 추출
            String capability1 = extractStringValue(jsonStr, "capability1");
            Integer score1 = extractIntegerValue(jsonStr, "score1");
            
            // capability2, score2 추출
            String capability2 = extractStringValue(jsonStr, "capability2");
            Integer score2 = extractIntegerValue(jsonStr, "score2");
            
            if (capability1 != null && score1 != null && score1 >= 1 && score1 <= 3) {
                result.put(capability1, score1);
            }
            
            if (capability2 != null && score2 != null && score2 >= 1 && score2 <= 3) {
                result.put(capability2, score2);
            }
            
            log.info("역량 분석 결과 파싱 완료 - {}개 역량 업데이트", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("ChatGPT 역량 분석 응답 파싱 실패: {}", response, e);
            throw new RuntimeException("역량 분석 데이터 파싱 실패");
        }
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
     * 정규식으로 Integer 값 추출 (역량 분석용)
     */
    private Integer extractIntegerValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
    
    /**
     * 기본 역량 업데이트 (ChatGPT 실패 시)
     */
    private Map<String, Integer> createDefaultCapabilityUpdates() {
        Map<String, Integer> result = new HashMap<>();
        
        // 기본값으로 finance와 operation_management에 각각 1점씩
        result.put("finance", 1);
        result.put("operation_management", 1);
        
        log.info("기본 역량 업데이트 적용 - finance: 1점, operation_management: 1점");
        return result;
    }
}
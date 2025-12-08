package com.example.chatgpt.service;

import com.example.chatgpt.dto.loanbusinessplan.reqDto.LoanBusinessPlanCreateReqDto;
import com.example.chatgpt.dto.loanbusinessplan.respDto.LoanAmountViewRespDto;
import com.example.chatgpt.dto.loanbusinessplan.respDto.LoanBusinessPlanDto;
import com.example.chatgpt.dto.loanbusinessplan.respDto.LoanBusinessPlanListRespDto;
import com.example.chatgpt.entity.LoanBusinessPlan;
import com.example.chatgpt.entity.Stage1Bizplan;
import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.repository.LoanBusinessPlanRepository;
import com.example.chatgpt.repository.Stage1BizplanRepository;
import com.example.chatgpt.repository.FinancialStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LoanBusinessPlanService {
    
    private final LoanBusinessPlanRepository loanBusinessPlanRepository;
    private final Stage1BizplanRepository stage1BizplanRepository;
    private final FinancialStatementRepository financialStatementRepository;
    private final BusinessPlanAnalyzer businessPlanAnalyzer; // ChatGPT API 호출
    
    /**
     * 대출 사업계획서 목록 조회
     */
    public LoanBusinessPlanListRespDto getLoanBusinessPlanList(Integer eventCode, Integer teamCode, Integer stageStep) {
        try {
            log.info("대출 사업계획서 목록 조회 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            // 1. 대출 사업계획서 조회
            Optional<LoanBusinessPlan> optionalPlan = loanBusinessPlanRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (optionalPlan.isEmpty()) {
                log.info("대출 사업계획서가 없음 - eventCode: {}, teamCode: {}, stageStep: {}", 
                         eventCode, teamCode, stageStep);
                return LoanBusinessPlanListRespDto.empty();
            }
            
            LoanBusinessPlan plan = optionalPlan.get();
            
            // 2. DTO 변환
            LoanBusinessPlanDto planDto = LoanBusinessPlanDto.from(plan);
            
            log.info("대출 사업계획서 조회 완료 - loanBizCode: {}, 제품명: {}", 
                     plan.getLoanBizCode(), plan.getProductOrServiceName());
            
            return LoanBusinessPlanListRespDto.from(List.of(planDto));
            
        } catch (Exception e) {
            log.error("대출 사업계획서 목록 조회 실패", e);
            throw new RuntimeException("대출 사업계획서 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 대출산정금액 조회
     */
    public LoanAmountViewRespDto getLoanAmount(Integer eventCode, Integer teamCode, Integer stageStep) {
        log.info("대출산정금액 조회 요청 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep);
        
        try {
            Optional<LoanBusinessPlan> optionalPlan = loanBusinessPlanRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (optionalPlan.isEmpty()) {
                throw new RuntimeException("해당 대출 사업계획서를 찾을 수 없습니다.");
            }
            
            LoanBusinessPlan plan = optionalPlan.get();
            LoanAmountViewRespDto result = LoanAmountViewRespDto.fromEntity(plan);
            
            log.info("대출산정금액 조회 성공 - 희망금액: {}만원, 산정금액: {}만원", 
                     plan.getDesiredLoanAmount(), plan.getCalculatedLoanAmount());
            return result;
            
        } catch (Exception e) {
            log.error("대출산정금액 조회 실패 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep, e);
            throw new RuntimeException("대출산정금액 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 대출 사업계획서 생성 및 대출금액 산정
     */
    @Transactional
    public Integer createLoanBusinessPlan(LoanBusinessPlanCreateReqDto request) {
        try {
            // 1. String 데이터를 Integer로 변환
            Integer eventCode = Integer.valueOf(request.getEventCode());
            Integer teamCode = Integer.valueOf(request.getTeamCode());
            Integer stageStep = Integer.valueOf(request.getStageStep());
            Integer desiredLoanAmount = parseInteger(request.getDesiredLoanAmount());
            
            log.info("대출 사업계획서 생성 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            // 2. 기존 데이터 확인 (중복 체크)
            Optional<LoanBusinessPlan> existingPlan = loanBusinessPlanRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            LoanBusinessPlan loanBusinessPlan;
            
            if (existingPlan.isPresent()) {
                // 3. 기존 데이터가 있으면 업데이트 (덮어쓰기)
                loanBusinessPlan = existingPlan.get();
                updateLoanBusinessPlanFields(loanBusinessPlan, request, desiredLoanAmount);
                log.info("기존 대출 사업계획서 업데이트 - loanBizCode: {}", loanBusinessPlan.getLoanBizCode());
                
            } else {
                // 4. 새로운 데이터 생성
                loanBusinessPlan = createNewLoanBusinessPlan(request, eventCode, teamCode, stageStep, desiredLoanAmount);
                log.info("새 대출 사업계획서 생성");
            }
            
            // 5. 저장 (calculated_loan_amount는 아직 null)
            LoanBusinessPlan savedPlan = loanBusinessPlanRepository.save(loanBusinessPlan);
            
            // 6. ChatGPT로 대출금액 산정
            Integer calculatedLoanAmount = calculateLoanAmountWithChatGPT(savedPlan, eventCode, teamCode);
            
            // 7. 산정된 대출금액 업데이트
            savedPlan.setCalculatedLoanAmount(calculatedLoanAmount);
            loanBusinessPlanRepository.save(savedPlan);
            
            // 8. ✅ 스테이지 3, 4인 경우 재무제표 생성
            if (stageStep == 3 || stageStep == 4) {
                createFinancialStatementForLoan(eventCode, teamCode, stageStep, calculatedLoanAmount);
            }
            
            log.info("대출 사업계획서 처리 완료 - loanBizCode: {}, 산정금액: {}만원", 
                     savedPlan.getLoanBizCode(), calculatedLoanAmount);
            
            return calculatedLoanAmount * 10000;
            
        } catch (NumberFormatException e) {
            log.error("숫자 변환 실패", e);
            throw new RuntimeException("잘못된 숫자 형식입니다: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("대출 사업계획서 처리 실패", e);
            throw new RuntimeException("대출 사업계획서 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * ChatGPT로 대출금액 산정
     */
    private Integer calculateLoanAmountWithChatGPT(LoanBusinessPlan plan, Integer eventCode, Integer teamCode) {
        try {
            log.info("ChatGPT 대출금액 산정 시작 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. Stage1 사업아이템 요약 조회
            String bizItemSummary = getBizItemSummary(eventCode, teamCode);
            
            // 2. ChatGPT 프롬프트 생성
            String prompt = createLoanCalculationPrompt(plan, bizItemSummary);
            
            // 3. ChatGPT API 호출
            String response = businessPlanAnalyzer.callChatGptApi(prompt);
            
            // 4. 응답 파싱
            Integer loanAmount = parseLoanAmountResponse(response);
            
            log.info("ChatGPT 대출금액 산정 완료 - 산정금액: {}만원", loanAmount);
            return loanAmount;
            
        } catch (Exception e) {
            log.error("ChatGPT 대출금액 산정 실패", e);
            // 실패시 0원 처리
            return 0;
        }
    }
    
    /**
     * Stage1 사업아이템 요약 조회
     */
    private String getBizItemSummary(Integer eventCode, Integer teamCode) {
        try {
            Optional<Stage1Bizplan> bizplan = stage1BizplanRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (bizplan.isPresent() && bizplan.get().getBizItemSummary() != null) {
                return bizplan.get().getBizItemSummary();
            } else {
                log.warn("사업아이템 요약을 찾을 수 없습니다 - eventCode: {}, teamCode: {}", eventCode, teamCode);
                return null; // null 처리 (ChatGPT가 대출계획서만으로 판단)
            }
            
        } catch (Exception e) {
            log.warn("사업아이템 요약 조회 실패", e);
            return null;
        }
    }
    
    /**
     * ChatGPT 대출금액 산정 프롬프트 생성
     */
    private String createLoanCalculationPrompt(LoanBusinessPlan plan, String bizItemSummary) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 은행의 대출 심사 전문가입니다.\n\n");
        prompt.append("# 대출금액 산정 요청\n\n");
        
        // 사업아이템 요약 (있는 경우만)
        if (bizItemSummary != null && !bizItemSummary.trim().isEmpty()) {
            prompt.append("## 사업아이템 요약\n");
            prompt.append(bizItemSummary).append("\n\n");
        }
        
        // 대출 사업계획서 정보
        prompt.append("## 대출 사업계획서\n");
        prompt.append("**제품/서비스명**: ").append(plan.getProductOrServiceName() != null ? plan.getProductOrServiceName() : "미기재").append("\n");
        prompt.append("**제품 용도 및 특성**: ").append(plan.getProductUsageFeatures() != null ? plan.getProductUsageFeatures() : "미기재").append("\n");
        prompt.append("**목표 고객층**: ").append(plan.getTargetCustomersIndustry() != null ? plan.getTargetCustomersIndustry() : "미기재").append("\n");
        prompt.append("**시장 규모**: ").append(plan.getMarketSizeGrowth() != null ? plan.getMarketSizeGrowth() : "미기재").append("\n");
        prompt.append("**경쟁 우위**: ").append(plan.getCompetitiveAdvantages() != null ? plan.getCompetitiveAdvantages() : "미기재").append("\n");
        prompt.append("**3년간 매출 전망**: ").append(plan.getSalesForecast3years() != null ? plan.getSalesForecast3years() : "미기재").append("\n");
        prompt.append("**판매전략**: ").append(plan.getSalesStrategyClientPlan() != null ? plan.getSalesStrategyClientPlan() : "미기재").append("\n");
        prompt.append("**자금활용계획**: ").append(plan.getFundUtilizationPlan() != null ? plan.getFundUtilizationPlan() : "미기재").append("\n");
        prompt.append("**희망 대출금액**: ").append(plan.getDesiredLoanAmount() != null ? String.format("%,d만원", plan.getDesiredLoanAmount()) : "미기재").append("\n\n");
        
        prompt.append("## 대출금액 산정 기준\n");
        prompt.append("- 사업의 현실성과 성장 가능성\n");
        prompt.append("- 시장 규모 및 경쟁력\n");
        prompt.append("- 매출 전망의 합리성\n");
        prompt.append("- 자금활용계획의 구체성\n");
        prompt.append("- 리스크 요인 평가\n\n");
        
        prompt.append("## 산정 범위\n");
        prompt.append("- 최소: 0만원 (대출 불가)\n");
        prompt.append("- 최대: 100,000만원 (10억원)\n\n");
        
        prompt.append("## 응답 형식\n");
        prompt.append("다음 JSON 형식으로 응답하세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"loan_amount\": 산정된대출금액(만원단위숫자만),\n");
        prompt.append("  \"reasoning\": \"산정 근거 설명\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("**중요**: 보수적이고 현실적으로 산정하세요. JSON 형식만 응답하세요.");
        
        return prompt.toString();
    }
    
    /**
     * ChatGPT 응답에서 대출금액 파싱
     */
    private Integer parseLoanAmountResponse(String response) {
        try {
            // JSON 추출
            String jsonStr = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            // loan_amount 추출 (정규식 사용)
            String pattern = "\"loan_amount\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(jsonStr);
            
            if (m.find()) {
                Integer loanAmount = Integer.parseInt(m.group(1));
                
                // 범위 체크 (0 ~ 100,000만원)
                if (loanAmount < 0) loanAmount = 0;
                if (loanAmount > 100000) loanAmount = 100000;
                
                return loanAmount;
            }
            
            throw new RuntimeException("대출금액 파싱 실패");
            
        } catch (Exception e) {
            log.error("ChatGPT 응답 파싱 실패: {}", response, e);
            throw new RuntimeException("대출금액 파싱 중 오류 발생");
        }
    }
    
    /**
     * 기존 대출 사업계획서 필드 업데이트
     */
    private void updateLoanBusinessPlanFields(LoanBusinessPlan plan, LoanBusinessPlanCreateReqDto request, Integer desiredLoanAmount) {
        plan.setProductOrServiceName(request.getProductOrServiceName());
        plan.setProductUsageFeatures(request.getProductUsageFeatures());
        plan.setProductSpecifications(request.getProductSpecifications());
        plan.setTargetCustomersIndustry(request.getTargetCustomersIndustry());
        plan.setMarketSizeGrowth(request.getMarketSizeGrowth());
        plan.setMajorClientsDistribution(request.getMajorClientsDistribution());
        plan.setCompetitorsProducts(request.getCompetitorsProducts());
        plan.setTechCapabilitiesCertifications(request.getTechCapabilitiesCertifications());
        plan.setQualityControlFacilities(request.getQualityControlFacilities());
        plan.setCompetitiveAdvantages(request.getCompetitiveAdvantages());
        plan.setSalesForecast3years(request.getSalesForecast3years());
        plan.setSalesStrategyClientPlan(request.getSalesStrategyClientPlan());
        plan.setMarketingStrategy(request.getMarketingStrategy());
        plan.setBizExecutionPlan(request.getBizExecutionPlan());
        plan.setFundUtilizationPlan(request.getFundUtilizationPlan());
        plan.setExpectedEffects(request.getExpectedEffects());
        plan.setDesiredLoanAmount(desiredLoanAmount);
        // calculated_loan_amount는 나중에 설정
    }
    
    /**
     * 새 대출 사업계획서 생성
     */
    private LoanBusinessPlan createNewLoanBusinessPlan(LoanBusinessPlanCreateReqDto request, Integer eventCode, 
                                                      Integer teamCode, Integer stageStep, Integer desiredLoanAmount) {
        return LoanBusinessPlan.builder()
            .eventCode(eventCode)
            .teamCode(teamCode)
            .stageStep(stageStep)
            .productOrServiceName(request.getProductOrServiceName())
            .productUsageFeatures(request.getProductUsageFeatures())
            .productSpecifications(request.getProductSpecifications())
            .targetCustomersIndustry(request.getTargetCustomersIndustry())
            .marketSizeGrowth(request.getMarketSizeGrowth())
            .majorClientsDistribution(request.getMajorClientsDistribution())
            .competitorsProducts(request.getCompetitorsProducts())
            .techCapabilitiesCertifications(request.getTechCapabilitiesCertifications())
            .qualityControlFacilities(request.getQualityControlFacilities())
            .competitiveAdvantages(request.getCompetitiveAdvantages())
            .salesForecast3years(request.getSalesForecast3years())
            .salesStrategyClientPlan(request.getSalesStrategyClientPlan())
            .marketingStrategy(request.getMarketingStrategy())
            .bizExecutionPlan(request.getBizExecutionPlan())
            .fundUtilizationPlan(request.getFundUtilizationPlan())
            .expectedEffects(request.getExpectedEffects())
            .desiredLoanAmount(desiredLoanAmount)
            .calculatedLoanAmount(null) // 나중에 설정
            .build();
    }
    
    /**
     * String을 Integer로 안전하게 변환
     */
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 대출 스테이지(3,4)용 재무제표 생성
     * 대출금액을 accounts_payable에 저장하고 나머지는 0으로 초기화
     */
    private void createFinancialStatementForLoan(Integer eventCode, Integer teamCode, Integer stageStep, Integer loanAmount) {
        try {
            log.info("대출용 재무제표 생성 시작 - eventCode: {}, teamCode: {}, stageStep: {}, 대출금액: {}만원", 
                     eventCode, teamCode, stageStep, loanAmount);
            
            // 1. 기존 재무제표 확인 (중복 방지)
            Optional<FinancialStatement> existingFs = financialStatementRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (existingFs.isPresent()) {
                // 기존 재무제표가 있으면 accounts_payable만 업데이트
                FinancialStatement fs = existingFs.get();
                fs.setAccountsPayable(loanAmount);
                fs.setUpdatedAt(LocalDateTime.now());
                financialStatementRepository.save(fs);
                log.info("기존 재무제표 대출금액 업데이트 완료 - 대출금액: {}만원", loanAmount);
                return;
            }
            
            // 2. 새로운 재무제표 생성 (대출금액은 accounts_payable에, 나머지는 0)
            FinancialStatement financialStatement = FinancialStatement.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stageStep)
                // 자산 항목들 - 모두 0
                .cashAndDeposits(0)
                .tangibleAssets(0)
                .inventoryAssets(0)
                .ppeAssets(0)
                .intangibleAssets(0)
                .totalAssets(0)
                // 부채 항목들 - 대출금액을 accounts_payable에
                .accountsPayable(loanAmount * 10000)
                .borrowings(0)
                .totalLiabilitiesEquity(0)
                // 자본 항목들 - 모두 0
                .capitalStock(0)
                // 손익계산서 항목들 - 모두 0
                .revenue(0)
                .cogs(0)
                .grossProfit(0)
                .sgnaExpenses(0)
                .rndExpenses(0)
                .operatingIncome(0)
                .nonOperatingIncome(0)
                .corporateTax(0)
                .netIncome(0)
                .fsScore(0)
                // 타임스탬프
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            // 3. 저장
            financialStatementRepository.save(financialStatement);
            
            log.info("대출용 재무제표 생성 완료 - 대출금액: {}만원", loanAmount);
            
        } catch (Exception e) {
            log.error("대출용 재무제표 생성 실패", e);
            // 재무제표 생성 실패해도 대출 사업계획서 저장은 유지 (에러 전파 안함)
        }
    }
}
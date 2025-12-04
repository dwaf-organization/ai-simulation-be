package com.example.chatgpt.dto.loanbusinessplan.respDto;

import com.example.chatgpt.entity.LoanBusinessPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanBusinessPlanDto {
    
    private Integer loanBizCode;                      // 대출 사업계획서 코드
    private Integer eventCode;                        // 행사 코드
    private Integer teamCode;                         // 팀 코드
    private Integer stageStep;                        // 스테이지 단계
    
    // 제품/서비스 정보
    private String productOrServiceName;              // 제품/서비스명
    private String productUsageFeatures;             // 제품 용도 및 특징
    private String productSpecifications;            // 제품 사양
    
    // 시장 정보
    private String targetCustomersIndustry;          // 대상 고객군 및 업계
    private String marketSizeGrowth;                 // 시장 규모 및 성장성
    private String majorClientsDistribution;         // 주요 고객사 및 유통망
    private String competitorsProducts;              // 경쟁사 및 경쟁 제품
    
    // 기술 및 경쟁력
    private String techCapabilitiesCertifications;   // 기술 역량 및 인증
    private String qualityControlFacilities;         // 품질관리 및 시설
    private String competitiveAdvantages;            // 경쟁 우위
    
    // 사업 계획
    private String salesForecast3years;              // 3년간 매출 예측
    private String salesStrategyClientPlan;          // 영업 전략 및 고객 확보 계획
    private String marketingStrategy;                // 마케팅 전략
    private String bizExecutionPlan;                 // 사업 실행 계획
    
    // 자금 계획
    private String fundUtilizationPlan;              // 자금 활용 계획
    private String expectedEffects;                  // 기대 효과
    private Integer desiredLoanAmount;               // 희망 대출 금액
    private Integer calculatedLoanAmount;            // 산정 대출 금액
    
    /**
     * Entity를 DTO로 변환 (날짜 필드 제외)
     */
    public static LoanBusinessPlanDto from(LoanBusinessPlan entity) {
        return LoanBusinessPlanDto.builder()
                .loanBizCode(entity.getLoanBizCode())
                .eventCode(entity.getEventCode())
                .teamCode(entity.getTeamCode())
                .stageStep(entity.getStageStep())
                .productOrServiceName(entity.getProductOrServiceName())
                .productUsageFeatures(entity.getProductUsageFeatures())
                .productSpecifications(entity.getProductSpecifications())
                .targetCustomersIndustry(entity.getTargetCustomersIndustry())
                .marketSizeGrowth(entity.getMarketSizeGrowth())
                .majorClientsDistribution(entity.getMajorClientsDistribution())
                .competitorsProducts(entity.getCompetitorsProducts())
                .techCapabilitiesCertifications(entity.getTechCapabilitiesCertifications())
                .qualityControlFacilities(entity.getQualityControlFacilities())
                .competitiveAdvantages(entity.getCompetitiveAdvantages())
                .salesForecast3years(entity.getSalesForecast3years())
                .salesStrategyClientPlan(entity.getSalesStrategyClientPlan())
                .marketingStrategy(entity.getMarketingStrategy())
                .bizExecutionPlan(entity.getBizExecutionPlan())
                .fundUtilizationPlan(entity.getFundUtilizationPlan())
                .expectedEffects(entity.getExpectedEffects())
                .desiredLoanAmount(entity.getDesiredLoanAmount())
                .calculatedLoanAmount(entity.getCalculatedLoanAmount())
                .build();
    }
}
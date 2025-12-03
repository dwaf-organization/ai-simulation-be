package com.example.chatgpt.dto.loanbusinessplan.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanBusinessPlanCreateReqDto {
    
    private String eventCode;
    private String teamCode;
    private String stageStep;
    
    // 사업계획서 상세 내용
    private String productOrServiceName;
    private String productUsageFeatures;
    private String productSpecifications;
    private String targetCustomersIndustry;
    private String marketSizeGrowth;
    private String majorClientsDistribution;
    private String competitorsProducts;
    private String techCapabilitiesCertifications;
    private String qualityControlFacilities;
    private String competitiveAdvantages;
    private String salesForecast3years;
    private String salesStrategyClientPlan;
    private String marketingStrategy;
    private String bizExecutionPlan;
    private String fundUtilizationPlan;
    private String expectedEffects;
    
    // 대출희망금액 (만원 단위로 받을 예정)
    private String desiredLoanAmount;
}
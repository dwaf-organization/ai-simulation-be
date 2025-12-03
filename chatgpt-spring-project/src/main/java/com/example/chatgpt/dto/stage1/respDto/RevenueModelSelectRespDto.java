package com.example.chatgpt.dto.stage1.respDto;

import com.example.chatgpt.entity.RevenueModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null이 아닌 필드만 JSON에 포함
public class RevenueModelSelectRespDto {
    
    // 기본 정보
    private Integer revenueModelCode;
    private Integer eventCode;
    private Integer teamCode;
    private Integer revenueCategory;
    
    // ====================================
    // 1. SaaS/구독 모델 (revenue_category = 1)
    // ====================================
    private Integer monthlySubscriptionFee;  // 월 구독료
    private Integer productDevInvestment;    // 제품 개발 투자액
    private Integer marketingBudget;         // 마케팅 예산
    private Integer customerSupportCost;     // 고객 지원 비용
    private Integer freeTrialPeriod;         // 무료 체험 기간
    
    // ====================================
    // 2. 플랫폼/중개 모델 (revenue_category = 2)
    // ====================================
    private Integer transactionFeeRate;           // 거래 수수료율
    private Integer supplierAcquisitionBudget;    // 공급자 유치 예산
    private Integer learnerAcquisitionBudget;     // 수요자 유치 예산
    private Integer platformFeatureInvest;        // 플랫폼 기능 개발 투자액
    private Integer trustSafetyInvest;            // 신뢰/안전 시스템 투자액
    
    // ====================================
    // 3. E-커머스 (revenue_category = 3)
    // ====================================
    private Integer avgSalePrice;              // 평균 판매 가격
    private Integer productCost;               // 상품 매입/제조 원가
    private Integer adMarketingBudget;         // 광고/마케팅 예산
    private Integer siteAppImproveInvest;      // 웹사이트/앱 개선 투자액
    private Integer logisticsInventoryInvest;  // 물류/재고 시스템 투자액
    
    // ====================================
    // 4. 서비스/에이전시 (revenue_category = 4)
    // ====================================
    private Integer hourlyChargeRate;      // 시간당 청구 금액
    private Integer salesActivityHours;    // 영업 활동 투입 시간
    private Integer projectExecutionHours; // 프로젝트 수행 투입 시간
    private Integer teamSkillInvest;       // 팀 역량 강화 투자액
    private Integer internalRndHours;      // 내부 R&D/자동화 투입 시간
    
    // ====================================
    // 5. 제조/하드웨어 (revenue_category = 5)
    // ====================================
    private Integer productRetailPrice;           // 제품 출고가/소비자가
    private Integer quarterlyProductionGoal;      // 분기 생산 목표량
    private Integer rndDesignInvest;             // R&D 및 디자인 투자액
    private Integer processImprovementInvest;    // 생산 공정 개선 투자액
    private Integer salesDistributionInvest;     // 영업 및 유통 채널 투자액
    
    // ====================================
    // 6. 광고 (revenue_category = 6)
    // ====================================
    private Integer contentProductionInvest;    // 콘텐츠 제작 투자액
    private Integer trafficAcquisitionBudget;   // 트래픽 확보 마케팅 예산
    private Integer adAgeTargetSettings;        // 광고 영업팀 투자액/시간
    private Integer adDensityPerPage;           // 페이지당 광고 밀도
    private Integer uxImprovementInvest;        // 사용자 경험(UX) 개선 투자액
    
    // ====================================
    // 7. 하이브리드 (revenue_category = 7)
    // ====================================
    private String primaryRevenueOption;    // 주력 수익모델 선택
    private String secondaryRevenueOption;  // 보조 수익모델 선택
    private Integer conversionRatio;        // 수익모델별 자원 배분율
    
    /**
     * Entity를 DTO로 변환 (null이 아닌 필드만 포함)
     */
    public static RevenueModelSelectRespDto from(RevenueModel model) {
        if (model == null) {
            return null;
        }
        
        return RevenueModelSelectRespDto.builder()
                // 기본 정보 (항상 포함)
                .revenueModelCode(model.getRevenueModelCode())
                .eventCode(model.getEventCode())
                .teamCode(model.getTeamCode())
                .revenueCategory(model.getRevenueCategory())
                
                // SaaS/구독 모델
                .monthlySubscriptionFee(model.getMonthlySubscriptionFee())
                .productDevInvestment(model.getProductDevInvestment())
                .marketingBudget(model.getMarketingBudget())
                .customerSupportCost(model.getCustomerSupportCost())
                .freeTrialPeriod(model.getFreeTrialPeriod())
                
                // 플랫폼/중개 모델
                .transactionFeeRate(model.getTransactionFeeRate())
                .supplierAcquisitionBudget(model.getSupplierAcquisitionBudget())
                .learnerAcquisitionBudget(model.getLearnerAcquisitionBudget())
                .platformFeatureInvest(model.getPlatformFeatureInvest())
                .trustSafetyInvest(model.getTrustSafetyInvest())
                
                // E-커머스
                .avgSalePrice(model.getAvgSalePrice())
                .productCost(model.getProductCost())
                .adMarketingBudget(model.getAdMarketingBudget())
                .siteAppImproveInvest(model.getSiteAppImproveInvest())
                .logisticsInventoryInvest(model.getLogisticsInventoryInvest())
                
                // 서비스/에이전시
                .hourlyChargeRate(model.getHourlyChargeRate())
                .salesActivityHours(model.getSalesActivityHours())
                .projectExecutionHours(model.getProjectExecutionHours())
                .teamSkillInvest(model.getTeamSkillInvest())
                .internalRndHours(model.getInternalRndHours())
                
                // 제조/하드웨어
                .productRetailPrice(model.getProductRetailPrice())
                .quarterlyProductionGoal(model.getQuarterlyProductionGoal())
                .rndDesignInvest(model.getRndDesignInvest())
                .processImprovementInvest(model.getProcessImprovementInvest())
                .salesDistributionInvest(model.getSalesDistributionInvest())
                
                // 광고
                .contentProductionInvest(model.getContentProductionInvest())
                .trafficAcquisitionBudget(model.getTrafficAcquisitionBudget())
                .adAgeTargetSettings(model.getAdAgeTargetSettings())
                .adDensityPerPage(model.getAdDensityPerPage())
                .uxImprovementInvest(model.getUxImprovementInvest())
                
                // 하이브리드
                .primaryRevenueOption(model.getPrimaryRevenueOption())
                .secondaryRevenueOption(model.getSecondaryRevenueOption())
                .conversionRatio(model.getConversionRatio())
                
                .build();
    }
}
package com.example.chatgpt.dto.stage1.reqDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueModelReqDto {
    
    @NotNull(message = "팀코드는 필수입니다.")
    private Integer teamCode;
    
    @NotNull(message = "수익 카테고리는 필수입니다.")
    @Min(value = 1, message = "수익 카테고리는 1-7 사이여야 합니다.")
    @Max(value = 7, message = "수익 카테고리는 1-7 사이여야 합니다.")
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
    private Integer transactionFeeRate;           // 거래 수수료율 (%)
    private Integer supplierAcquisitionBudget;    // 공급자 유치 예산
    private Integer learnerAcquisitionBudget;     // 수요자 유치 예산
    private Integer platformFeatureInvest;        // 플랫폼 기능 개발 투자액
    private Integer trustSafetyInvest;            // 신뢰/안전 시스템 투자액
    
    // ====================================
    // 3. E-커머스 (revenue_category = 3)
    // ====================================
    private Integer avgSalePrice;                 // 평균 판매 가격
    private Integer productCost;                  // 상품 매입/제조 원가
    private Integer adMarketingBudget;            // 광고/마케팅 예산
    private Integer siteAppImproveInvest;         // 웹사이트/앱 개선 투자액
    private Integer logisticsInventoryInvest;     // 물류/재고 시스템 투자액
    
    // ====================================
    // 4. 서비스/에이전시 (revenue_category = 4)
    // ====================================
    private Integer hourlyChargeRate;             // 시간당 청구 금액
    private Integer salesActivityHours;           // 영업 활동 투입 시간
    private Integer projectExecutionHours;        // 프로젝트 수행 투입 시간
    private Integer teamSkillInvest;              // 팀 역량 강화 투자액
    private Integer internalRndHours;             // 내부 R&D/자동화 투입 시간
    
    // ====================================
    // 5. 제조/하드웨어 (revenue_category = 5)
    // ====================================
    private Integer productRetailPrice;           // 제품 출고가/소비자가
    private Integer quarterlyProductionGoal;      // 분기 생산 목표량
    private Integer rndDesignInvest;              // R&D 및 디자인 투자액
    private Integer processImprovementInvest;     // 생산 공정 개선 투자액
    private Integer salesDistributionInvest;      // 영업 및 유통 채널 투자액
    
    // ====================================
    // 6. 광고 (revenue_category = 6)
    // ====================================
    private Integer contentProductionInvest;      // 콘텐츠 제작 투자액
    private Integer trafficAcquisitionBudget;     // 트래픽 확보 마케팅 예산
    private Integer adAgeTargetSettings;          // 광고 영업팀 투자액/시간
    private Integer adDensityPerPage;             // 페이지당 광고 밀도
    private Integer uxImprovementInvest;          // 사용자 경험(UX) 개선 투자액
    
    // ====================================
    // 7. 하이브리드 (revenue_category = 7)
    // ====================================
    private String primaryRevenueOption;          // 주력 수익모델 선택
    private String secondaryRevenueOption;        // 보조 수익모델 선택
    private Integer conversionRatio;              // 수익모델별 자원 배분율 (%)
}
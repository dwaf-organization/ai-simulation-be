package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_model")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revenue_model_code")
    private Integer revenueModelCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "revenue_category")
    private Integer revenueCategory;  // 1-7: SaaS, 플랫폼, 이커머스, 서비스, 제조, 광고, 하이브리드
    
    // ====================================
    // 1. SaaS/구독 모델 (revenue_category = 1)
    // ====================================
    @Column(name = "monthly_subscription_fee")
    private Integer monthlySubscriptionFee;  // 월 구독료
    
    @Column(name = "product_dev_investment")
    private Integer productDevInvestment;  // 제품 개발 투자액
    
    @Column(name = "marketing_budget")
    private Integer marketingBudget;  // 마케팅 예산
    
    @Column(name = "customer_support_cost")
    private Integer customerSupportCost;  // 고객 지원 비용
    
    @Column(name = "free_trial_period")
    private Integer freeTrialPeriod;  // 무료 체험 기간
    
    // ====================================
    // 3. E-커머스 (revenue_category = 3)
    // ====================================
    @Column(name = "avg_sale_price")
    private Integer avgSalePrice;  // 평균 판매 가격
    
    @Column(name = "product_cost")
    private Integer productCost;  // 상품 매입/제조 원가
    
    @Column(name = "ad_marketing_budget")
    private Integer adMarketingBudget;  // 광고/마케팅 예산
    
    @Column(name = "site_app_improve_invest")
    private Integer siteAppImproveInvest;  // 웹사이트/앱 개선 투자액
    
    @Column(name = "logistics_inventory_invest")
    private Integer logisticsInventoryInvest;  // 물류/재고 시스템 투자액
    
    // ====================================
    // 2. 플랫폼/중개 모델 (revenue_category = 2)
    // ====================================
    @Column(name = "transaction_fee_rate")
    private Integer transactionFeeRate;  // 거래 수수료율 (%)
    
    @Column(name = "supplier_acquisition_budget")
    private Integer supplierAcquisitionBudget;  // 공급자 유치 예산
    
    @Column(name = "learner_acquisition_budget")
    private Integer learnerAcquisitionBudget;  // 수요자 유치 예산
    
    @Column(name = "platform_feature_invest")
    private Integer platformFeatureInvest;  // 플랫폼 기능 개발 투자액
    
    @Column(name = "trust_safety_invest")
    private Integer trustSafetyInvest;  // 신뢰/안전 시스템 투자액
    
    // ====================================
    // 5. 제조/하드웨어 (revenue_category = 5)
    // ====================================
    @Column(name = "product_retail_price")
    private Integer productRetailPrice;  // 제품 출고가/소비자가
    
    @Column(name = "quarterly_production_goal")
    private Integer quarterlyProductionGoal;  // 분기 생산 목표량
    
    @Column(name = "rnd_design_invest")
    private Integer rndDesignInvest;  // R&D 및 디자인 투자액
    
    @Column(name = "process_improvement_invest")
    private Integer processImprovementInvest;  // 생산 공정 개선 투자액
    
    @Column(name = "sales_distribution_invest")
    private Integer salesDistributionInvest;  // 영업 및 유통 채널 투자액
    
    // ====================================
    // 4. 서비스/에이전시 (revenue_category = 4)
    // ====================================
    @Column(name = "hourly_charge_rate")
    private Integer hourlyChargeRate;  // 시간당 청구 금액
    
    @Column(name = "sales_activity_hours")
    private Integer salesActivityHours;  // 영업 활동 투입 시간
    
    @Column(name = "project_execution_hours")
    private Integer projectExecutionHours;  // 프로젝트 수행 투입 시간
    
    @Column(name = "team_skill_invest")
    private Integer teamSkillInvest;  // 팀 역량 강화 투자액
    
    @Column(name = "internal_rnd_hours")
    private Integer internalRndHours;  // 내부 R&D/자동화 투입 시간
    
    // ====================================
    // 6. 광고 (revenue_category = 6)
    // ====================================
    @Column(name = "content_production_invest")
    private Integer contentProductionInvest;  // 콘텐츠 제작 투자액
    
    @Column(name = "traffic_acquisition_budget")
    private Integer trafficAcquisitionBudget;  // 트래픽 확보 마케팅 예산
    
    @Column(name = "ad_age_target_settings")
    private Integer adAgeTargetSettings;  // 광고 영업팀 투자액/시간
    
    @Column(name = "ad_density_per_page")
    private Integer adDensityPerPage;  // 페이지당 광고 밀도
    
    @Column(name = "ux_improvement_invest")
    private Integer uxImprovementInvest;  // 사용자 경험(UX) 개선 투자액
    
    // ====================================
    // 7. 하이브리드 (revenue_category = 7)
    // ====================================
    @Column(name = "primary_revenue_option")
    private String primaryRevenueOption;  // 주력 수익모델 선택
    
    @Column(name = "secondary_revenue_option")
    private String secondaryRevenueOption;  // 보조 수익모델 선택
    
    @Column(name = "conversion_ratio")
    private Integer conversionRatio;  // 수익모델별 자원 배분율 (%)
    
    // ====================================
    // 공통 필드
    // ====================================
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_business_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanBusinessPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_biz_code")
    private Integer loanBizCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;
    
    @Column(name = "product_or_service_name", length = 250)
    private String productOrServiceName;
    
    @Column(name = "product_usage_features")
    @Lob
    private String productUsageFeatures;
    
    @Column(name = "product_specifications")
    @Lob
    private String productSpecifications;
    
    @Column(name = "target_customers_industry")
    @Lob
    private String targetCustomersIndustry;
    
    @Column(name = "market_size_growth")
    @Lob
    private String marketSizeGrowth;
    
    @Column(name = "major_clients_distribution")
    @Lob
    private String majorClientsDistribution;
    
    @Column(name = "competitors_products")
    @Lob
    private String competitorsProducts;
    
    @Column(name = "tech_capabilities_certifications")
    @Lob
    private String techCapabilitiesCertifications;
    
    @Column(name = "quality_control_facilities")
    @Lob
    private String qualityControlFacilities;
    
    @Column(name = "competitive_advantages")
    @Lob
    private String competitiveAdvantages;
    
    @Column(name = "sales_forecast_3years")
    @Lob
    private String salesForecast3years;
    
    @Column(name = "sales_strategy_client_plan")
    @Lob
    private String salesStrategyClientPlan;
    
    @Column(name = "marketing_strategy")
    @Lob
    private String marketingStrategy;
    
    @Column(name = "biz_execution_plan")
    @Lob
    private String bizExecutionPlan;
    
    @Column(name = "fund_utilization_plan")
    @Lob
    private String fundUtilizationPlan;
    
    @Column(name = "expected_effects")
    @Lob
    private String expectedEffects;
    
    @Column(name = "desired_loan_amount")
    private Integer desiredLoanAmount;
    
    @Column(name = "calculated_loan_amount")
    private Integer calculatedLoanAmount;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
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
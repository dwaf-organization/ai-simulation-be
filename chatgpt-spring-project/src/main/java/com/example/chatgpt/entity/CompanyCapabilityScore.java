package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_capability_score")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCapabilityScore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capability_score_code")
    private Integer capabilityScoreCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "strategy_capability", nullable = false)
    @Builder.Default
    private Integer strategyCapability = 0;
    
    @Column(name = "finance_capability", nullable = false)
    @Builder.Default
    private Integer financeCapability = 0;
    
    @Column(name = "market_customer_capability", nullable = false)
    @Builder.Default
    private Integer marketCustomerCapability = 0;
    
    @Column(name = "operation_management_capability", nullable = false)
    @Builder.Default
    private Integer operationManagementCapability = 0;
    
    @Column(name = "technology_innovation_capability", nullable = false)
    @Builder.Default
    private Integer technologyInnovationCapability = 0;
    
    @Column(name = "sustainability_capability", nullable = false)
    @Builder.Default
    private Integer sustainabilityCapability = 0;
    
    @Column(name = "total_capability_level", nullable = false)
    @Builder.Default
    private Integer totalCapabilityLevel = 0;
    
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
    
    /**
     * 역량 점수 업데이트 (누적)
     */
    public void addCapabilityScore(String capabilityType, Integer score) {
        switch (capabilityType.toLowerCase()) {
            case "strategy":
                this.strategyCapability += score;
                break;
            case "finance":
                this.financeCapability += score;
                break;
            case "market_customer":
                this.marketCustomerCapability += score;
                break;
            case "operation_management":
                this.operationManagementCapability += score;
                break;
            case "technology_innovation":
                this.technologyInnovationCapability += score;
                break;
            case "sustainability":
                this.sustainabilityCapability += score;
                break;
        }
        
        // 총 역량 레벨 재계산
        calculateTotalCapabilityLevel();
    }
    
    /**
     * 총 역량 레벨 계산 (모든 역량의 합계)
     */
    private void calculateTotalCapabilityLevel() {
        this.totalCapabilityLevel = strategyCapability + 
                                  financeCapability + 
                                  marketCustomerCapability + 
                                  operationManagementCapability + 
                                  technologyInnovationCapability + 
                                  sustainabilityCapability;
    }
    
    /**
     * 기본 데이터 생성 (팀 생성시)
     */
    public static CompanyCapabilityScore createDefault(Integer eventCode, Integer teamCode) {
        return CompanyCapabilityScore.builder()
            .eventCode(eventCode)
            .teamCode(teamCode)
            .strategyCapability(0)
            .financeCapability(0)
            .marketCustomerCapability(0)
            .operationManagementCapability(0)
            .technologyInnovationCapability(0)
            .sustainabilityCapability(0)
            .totalCapabilityLevel(0)
            .build();
    }
}
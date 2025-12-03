package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// Spring Boot 3.0+ (Jakarta EE)
import jakarta.persistence.*;

// Spring Boot 2.x 이하라면 아래 주석을 해제하고 위 import를 제거하세요
// import javax.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_statement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fs_code")
    private Integer fsCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "stage_step")
    private Integer stageStep;
    
    // 자산 항목들
    @Column(name = "cash_and_deposits")
    private Integer cashAndDeposits; // 현금및예금
    
    @Column(name = "tangible_assets")
    private Integer tangibleAssets; // 유형자산
    
    @Column(name = "inventory_assets")
    private Integer inventoryAssets; // 재고자산
    
    @Column(name = "ppe_assets")
    private Integer ppeAssets; // 유형자산(PPE)
    
    @Column(name = "intangible_assets")
    private Integer intangibleAssets; // 무형자산
    
    // 부채 항목들
    @Column(name = "accounts_payable")
    private Integer accountsPayable; // 매입채무
    
    @Column(name = "borrowings")
    private Integer borrowings; // 차입금
    
    // 자본 항목들
    @Column(name = "capital_stock")
    private Integer capitalStock; // 자본금
    
    @Column(name = "total_assets")
    private Integer totalAssets; // 자산총계
    
    @Column(name = "total_liabilities_equity")
    private Integer totalLiabilitiesEquity; // 부채및자본총계
    
    // 손익계산서 항목들
    @Column(name = "revenue")
    private Integer revenue; // 매출액
    
    @Column(name = "cogs")
    private Integer cogs; // 매출원가
    
    @Column(name = "gross_profit")
    private Integer grossProfit; // 매출총이익
    
    @Column(name = "sgna_expenses")
    private Integer sgnaExpenses; // 판매비와관리비
    
    @Column(name = "rnd_expenses")
    private Integer rndExpenses; // 연구개발비
    
    @Column(name = "operating_income")
    private Integer operatingIncome; // 영업이익
    
    @Column(name = "non_operating_income")
    private Integer nonOperatingIncome; // 영업외수익
    
    @Column(name = "corporate_tax")
    private Integer corporateTax; // 법인세
    
    @Column(name = "net_income")
    private Integer netIncome; // 순이익
    
    @Column(name = "fs_score")
    private Integer fsScore; // 재무점수
    
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
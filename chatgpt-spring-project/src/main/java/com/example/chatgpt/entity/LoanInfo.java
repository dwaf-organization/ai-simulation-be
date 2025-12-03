package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_code")
    private Integer loanCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "bank_code", nullable = false)
    private Integer bankCode;
    
    @Column(name = "loan_type", nullable = false, length = 50)
    private String loanType;
    
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;
    
    @Column(name = "biz_reg_doc_path", length = 250)
    private String bizRegDocPath;
    
    @Column(name = "corp_reg_doc_path", length = 250)
    private String corpRegDocPath;
    
    @Column(name = "shareholder_list_doc_path", length = 250)
    private String shareholderListDocPath;
    
    @Column(name = "financial_statement_doc_path", length = 250)
    private String financialStatementDocPath;
    
    @Column(name = "vat_tax_doc_path", length = 250)
    private String vatTaxDocPath;
    
    @Column(name = "social_insurance_doc_path", length = 250)
    private String socialInsuranceDocPath;
    
    @Column(name = "tax_payment_doc_path", length = 250)
    private String taxPaymentDocPath;
    
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
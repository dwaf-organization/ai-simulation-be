package com.example.chatgpt.dto.loaninfo.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanInfoCreateReqDto {
    
    private String eventCode;
    private String teamCode;
    private String stageStep;
    private String bankCode;
    private String loanType;
    
    // Firebase 경로들 (선택적)
    private String bizRegDocPath;
    private String corpRegDocPath;
    private String shareholderListDocPath;
    private String financialStatementDocPath;
    private String vatTaxDocPath;
    private String socialInsuranceDocPath;
    private String taxPaymentDocPath;
}
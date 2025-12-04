package com.example.chatgpt.dto.loaninfo.respDto;

import com.example.chatgpt.entity.LoanInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInfoDto {
    
    private Integer loanCode;                    // 대출 코드
    private Integer eventCode;                   // 행사 코드
    private Integer teamCode;                    // 팀 코드
    private Integer bankCode;                    // 은행 코드
    private String loanType;                     // 대출 타입
    private Integer stageStep;                   // 스테이지 단계
    private String bizRegDocPath;                // 사업자등록증
    private String corpRegDocPath;               // 법인등기부등본
    private String shareholderListDocPath;       // 주주명부
    private String financialStatementDocPath;    // 재무제표
    private String vatTaxDocPath;                // 부가세 신고서
    private String socialInsuranceDocPath;       // 사회보험료 납부확인서
    private String taxPaymentDocPath;            // 세금 납부증명서
    private String bank;                         // 은행명 (Bank 테이블에서 조인)
    
    /**
     * Entity를 DTO로 변환 (bank 필드는 별도 설정)
     */
    public static LoanInfoDto from(LoanInfo loanInfo, String bankName) {
        return LoanInfoDto.builder()
                .loanCode(loanInfo.getLoanCode())
                .eventCode(loanInfo.getEventCode())
                .teamCode(loanInfo.getTeamCode())
                .bankCode(loanInfo.getBankCode())
                .loanType(loanInfo.getLoanType())
                .stageStep(loanInfo.getStageStep())
                .bizRegDocPath(loanInfo.getBizRegDocPath())
                .corpRegDocPath(loanInfo.getCorpRegDocPath())
                .shareholderListDocPath(loanInfo.getShareholderListDocPath())
                .financialStatementDocPath(loanInfo.getFinancialStatementDocPath())
                .vatTaxDocPath(loanInfo.getVatTaxDocPath())
                .socialInsuranceDocPath(loanInfo.getSocialInsuranceDocPath())
                .taxPaymentDocPath(loanInfo.getTaxPaymentDocPath())
                .bank(bankName)
                .build();
    }
}
package com.example.chatgpt.dto.financialstatement.respDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailableAmountRespDto {
    
    private Integer eventCode;
    private Integer teamCode;
    private Integer currentStageStep;
    private Integer previousCashAndDeposits;  // 전 스테이지 현금및예금
    private Integer currentAccountsPayable;   // 현재 스테이지 매입채무
    private Integer availableAmount;          // 사용가능금액 (두 값의 합)
    
    public static AvailableAmountRespDto of(Integer eventCode, 
                                          Integer teamCode, 
                                          Integer currentStageStep, 
                                          Integer previousCashAndDeposits, 
                                          Integer currentAccountsPayable) {
        
        Integer availableAmount = (previousCashAndDeposits != null ? previousCashAndDeposits : 0) + 
                                (currentAccountsPayable != null ? currentAccountsPayable : 0);
        
        return AvailableAmountRespDto.builder()
            .eventCode(eventCode)
            .teamCode(teamCode)
            .currentStageStep(currentStageStep)
            .previousCashAndDeposits(previousCashAndDeposits)
            .currentAccountsPayable(currentAccountsPayable)
            .availableAmount(availableAmount)
            .build();
    }
}
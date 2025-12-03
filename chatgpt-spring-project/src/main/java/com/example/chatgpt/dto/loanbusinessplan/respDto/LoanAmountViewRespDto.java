package com.example.chatgpt.dto.loanbusinessplan.respDto;

import com.example.chatgpt.entity.LoanBusinessPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAmountViewRespDto {
    
    private Integer desiredLoanAmount;
    private Integer calculatedLoanAmount;
    
    /**
     * Entity → DTO 변환
     */
    public static LoanAmountViewRespDto fromEntity(LoanBusinessPlan loanBusinessPlan) {
        return LoanAmountViewRespDto.builder()
                .desiredLoanAmount(loanBusinessPlan.getDesiredLoanAmount())
                .calculatedLoanAmount(loanBusinessPlan.getCalculatedLoanAmount())
                .build();
    }
}
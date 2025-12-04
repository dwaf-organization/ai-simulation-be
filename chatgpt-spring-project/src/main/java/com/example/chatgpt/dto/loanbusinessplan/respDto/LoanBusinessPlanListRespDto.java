package com.example.chatgpt.dto.loanbusinessplan.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanBusinessPlanListRespDto {
    
    private List<LoanBusinessPlanDto> loanBusinessPlans;  // 대출 사업계획서 목록
    
    /**
     * 대출 사업계획서 목록으로 DTO 생성
     */
    public static LoanBusinessPlanListRespDto from(List<LoanBusinessPlanDto> loanBusinessPlans) {
        return LoanBusinessPlanListRespDto.builder()
                .loanBusinessPlans(loanBusinessPlans)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static LoanBusinessPlanListRespDto empty() {
        return LoanBusinessPlanListRespDto.builder()
                .loanBusinessPlans(List.of())
                .build();
    }
}
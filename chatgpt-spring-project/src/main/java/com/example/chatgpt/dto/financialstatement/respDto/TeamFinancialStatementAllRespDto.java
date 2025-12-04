package com.example.chatgpt.dto.financialstatement.respDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 값은 JSON에서 제외
public class TeamFinancialStatementAllRespDto {
    
    private Integer eventCode;                      // 이벤트 코드
    private Integer teamCode;                       // 팀 코드
    
    private FinancialStatementDto stage2;           // 2단계 재무제표
    private FinancialStatementDto stage3;           // 3단계 재무제표
    private FinancialStatementDto stage4;           // 4단계 재무제표
    private FinancialStatementDto stage5;           // 5단계 재무제표
    private FinancialStatementDto stage6;           // 6단계 재무제표
    private FinancialStatementDto stage7;           // 7단계 재무제표
    
    /**
     * 빈 응답 생성 (모든 스테이지 null)
     */
    public static TeamFinancialStatementAllRespDto empty(Integer eventCode, Integer teamCode) {
        return TeamFinancialStatementAllRespDto.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stage2(null)
                .stage3(null)
                .stage4(null)
                .stage5(null)
                .stage6(null)
                .stage7(null)
                .build();
    }
}
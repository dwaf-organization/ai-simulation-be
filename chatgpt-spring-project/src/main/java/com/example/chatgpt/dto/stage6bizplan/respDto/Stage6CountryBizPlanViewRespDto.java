package com.example.chatgpt.dto.stage6bizplan.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage6CountryBizPlanViewRespDto {
    
    private String countrySummary;  // 해당 국가의 사업계획서 요약
    
    /**
     * 성공 응답 생성
     */
    public static Stage6CountryBizPlanViewRespDto success(String countrySummary) {
        return Stage6CountryBizPlanViewRespDto.builder()
                .countrySummary(countrySummary)
                .build();
    }
}
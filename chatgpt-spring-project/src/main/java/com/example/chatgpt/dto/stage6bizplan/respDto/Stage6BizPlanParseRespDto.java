package com.example.chatgpt.dto.stage6bizplan.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage6BizPlanParseRespDto {
    
    private String parsedContent;  // 파싱된 텍스트 내용
    
    /**
     * 성공 응답 생성
     */
    public static Stage6BizPlanParseRespDto success(String parsedContent) {
        return Stage6BizPlanParseRespDto.builder()
                .parsedContent(parsedContent)
                .build();
    }
}
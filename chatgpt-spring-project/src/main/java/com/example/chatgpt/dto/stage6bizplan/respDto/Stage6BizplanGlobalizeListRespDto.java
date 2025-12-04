package com.example.chatgpt.dto.stage6bizplan.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage6BizplanGlobalizeListRespDto {
    
    private List<Stage6BizplanGlobalizeDto> globalBizplans;  // 글로벌 사업계획서 목록
    
    /**
     * 글로벌 사업계획서 목록으로 DTO 생성
     */
    public static Stage6BizplanGlobalizeListRespDto from(List<Stage6BizplanGlobalizeDto> globalBizplans) {
        return Stage6BizplanGlobalizeListRespDto.builder()
                .globalBizplans(globalBizplans)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static Stage6BizplanGlobalizeListRespDto empty() {
        return Stage6BizplanGlobalizeListRespDto.builder()
                .globalBizplans(List.of())
                .build();
    }
}
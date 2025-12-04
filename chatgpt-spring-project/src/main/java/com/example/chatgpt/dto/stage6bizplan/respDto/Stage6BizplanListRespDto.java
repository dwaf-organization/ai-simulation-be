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
public class Stage6BizplanListRespDto {
    
    private List<Stage6BizplanDto> bizplans;  // 사업계획서 목록
    
    /**
     * 사업계획서 목록으로 DTO 생성
     */
    public static Stage6BizplanListRespDto from(List<Stage6BizplanDto> bizplans) {
        return Stage6BizplanListRespDto.builder()
                .bizplans(bizplans)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static Stage6BizplanListRespDto empty() {
        return Stage6BizplanListRespDto.builder()
                .bizplans(List.of())
                .build();
    }
}
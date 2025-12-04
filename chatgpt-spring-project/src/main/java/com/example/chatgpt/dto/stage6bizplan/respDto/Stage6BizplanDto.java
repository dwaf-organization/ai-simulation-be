package com.example.chatgpt.dto.stage6bizplan.respDto;

import com.example.chatgpt.entity.Stage6BizplanSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage6BizplanDto {
    
    private Integer stage6Code;          // Stage6 코드
    private Integer eventCode;           // 행사 코드  
    private Integer teamCode;            // 팀 코드
    private String bizplanFilePath;      // 사업계획서 파일 경로
    private String bizItemSummary;       // 사업 아이템 요약
    
    /**
     * Entity를 일반 사업계획서 DTO로 변환
     */
    public static Stage6BizplanDto from(Stage6BizplanSummary entity) {
        return Stage6BizplanDto.builder()
                .stage6Code(entity.getStage6Code())
                .eventCode(entity.getEventCode())
                .teamCode(entity.getTeamCode())
                .bizplanFilePath(entity.getBizplanFilePath())
                .bizItemSummary(entity.getBizItemSummary())
                .build();
    }
}
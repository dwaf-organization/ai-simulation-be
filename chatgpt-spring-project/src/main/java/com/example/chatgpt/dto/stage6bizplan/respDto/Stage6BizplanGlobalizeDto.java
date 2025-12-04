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
public class Stage6BizplanGlobalizeDto {
    
    private Integer stage6Code;              // Stage6 코드
    private Integer eventCode;               // 행사 코드
    private Integer teamCode;                // 팀 코드
    private String globalBizplanFilePath;    // 글로벌 사업계획서 파일 경로
    private String globalBizItemSummary;     // 글로벌 사업 아이템 요약
    
    /**
     * Entity를 글로벌 사업계획서 DTO로 변환
     */
    public static Stage6BizplanGlobalizeDto from(Stage6BizplanSummary entity) {
        return Stage6BizplanGlobalizeDto.builder()
                .stage6Code(entity.getStage6Code())
                .eventCode(entity.getEventCode())
                .teamCode(entity.getTeamCode())
                .globalBizplanFilePath(entity.getGlobalBizplanFilePath())
                .globalBizItemSummary(entity.getGlobalBizItemSummary())
                .build();
    }
}
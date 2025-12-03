package com.example.chatgpt.dto.stage.respDto;

import com.example.chatgpt.entity.TeamMst;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentStageInfoRespDto {
    
    private Integer eventCode;        // 행사 코드
    private Integer teamCode;         // 팀 코드
    private Integer currentStageId;   // 현재 스테이지 ID
    private Integer currentStepId;    // 현재 스텝 ID
    private List<StageInfoDto> stages; // 현재 스테이지의 모든 스텝 정보
    
    /**
     * TeamMst와 Stage 정보로 DTO 생성
     */
    public static CurrentStageInfoRespDto from(TeamMst teamMst, List<StageInfoDto> stages) {
        return CurrentStageInfoRespDto.builder()
                .eventCode(teamMst.getEventCode())
                .teamCode(teamMst.getTeamCode())
                .currentStageId(teamMst.getCurrentStageId())
                .currentStepId(teamMst.getCurrentStepId())
                .stages(stages)
                .build();
    }
}
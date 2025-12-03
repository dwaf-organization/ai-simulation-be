package com.example.chatgpt.dto.team.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.chatgpt.entity.TeamMst;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateRespDto {
    
    private Integer teamCode;
    private Integer eventCode;
    private String teamId;
    private String teamName;
    private String teamImgUrl;
    private String teamLeaderName;
    
    public static TeamCreateRespDto from(TeamMst teamMst) {
        return TeamCreateRespDto.builder()
            .teamCode(teamMst.getTeamCode())
            .eventCode(teamMst.getEventCode())
            .teamId(teamMst.getTeamId())
            .teamName(teamMst.getTeamName())
            .teamImgUrl(teamMst.getTeamImageUrl())
            .teamLeaderName(teamMst.getTeamLeaderName())
            .build();
    }
}
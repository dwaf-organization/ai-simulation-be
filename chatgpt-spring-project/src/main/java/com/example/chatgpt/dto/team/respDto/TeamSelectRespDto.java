package com.example.chatgpt.dto.team.respDto;

import com.example.chatgpt.entity.TeamDtl;
import com.example.chatgpt.entity.TeamMst;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class TeamSelectRespDto {
    
    private Integer eventCode;
    private Integer teamCode;
    private String teamName;
    private String teamLeaderName;
    private String teamImageUrl;
    private List<String> members;
    
    public static TeamSelectRespDto from(TeamMst teamMst, List<TeamDtl> teamMembers) {
        List<String> memberNames = teamMembers.stream()
                .map(TeamDtl::getTeamMemberName)
                .collect(Collectors.toList());
        
        return TeamSelectRespDto.builder()
                .eventCode(teamMst.getEventCode())
                .teamCode(teamMst.getTeamCode())
                .teamName(teamMst.getTeamName())
                .teamLeaderName(teamMst.getTeamLeaderName())
                .teamImageUrl(teamMst.getTeamImageUrl())
                .members(memberNames)
                .build();
    }
}
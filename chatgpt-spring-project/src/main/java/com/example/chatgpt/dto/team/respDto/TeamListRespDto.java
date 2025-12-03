package com.example.chatgpt.dto.team.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.chatgpt.common.dto.PaginationDto;
import com.example.chatgpt.entity.TeamMst;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamListRespDto {
    
    private List<TeamItem> content;
    private PaginationDto pagination;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamItem {
        
        private Integer teamCode;
        private Integer eventCode;
        private String eventName;
        private String teamId;
        private String teamName;
        private String teamImgUrl;
        private String teamLeaderName;
        
        public static TeamItem from(TeamMst teamMst, String eventName) {
            return TeamItem.builder()
                .teamCode(teamMst.getTeamCode())
                .eventCode(teamMst.getEventCode())
                .eventName(eventName)
                .teamId(teamMst.getTeamId())
                .teamName(teamMst.getTeamName())
                .teamImgUrl(teamMst.getTeamImageUrl())
                .teamLeaderName(teamMst.getTeamLeaderName())
                .build();
        }
    }
}
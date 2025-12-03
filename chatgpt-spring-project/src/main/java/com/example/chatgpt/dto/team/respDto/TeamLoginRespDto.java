package com.example.chatgpt.dto.team.respDto;

import com.example.chatgpt.entity.TeamMst;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamLoginRespDto {
    
    private Integer eventCode;  // 행사 코드
    private Integer teamCode;   // 팀 코드
    
    /**
     * TeamMst Entity를 DTO로 변환
     */
    public static TeamLoginRespDto from(TeamMst teamMst) {
        return TeamLoginRespDto.builder()
                .eventCode(teamMst.getEventCode())
                .teamCode(teamMst.getTeamCode())
                .build();
    }
}
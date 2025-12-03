package com.example.chatgpt.dto.team.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDeleteRespDto {
    
    private Integer teamCode;
    private Integer deletedTeamMembers;
    
    public static TeamDeleteRespDto from(Integer teamCode, Integer deletedTeamMembers) {
        return TeamDeleteRespDto.builder()
            .teamCode(teamCode)
            .deletedTeamMembers(deletedTeamMembers)
            .build();
    }
}
package com.example.chatgpt.dto.stageranking.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageRankingRespDto {
    
    private Integer teamCode;
    private Integer allocatedRevenue;
    private Integer stageRank;
    private Integer fsScore;
    private Integer totalCapabilityLevel;
    
    /**
     * 기본값으로 생성 (조인 결과가 없는 경우)
     */
    public static StageRankingRespDto createDefault(Integer teamCode) {
        return StageRankingRespDto.builder()
                .teamCode(teamCode)
                .allocatedRevenue(null)
                .stageRank(null)
                .fsScore(null)
                .totalCapabilityLevel(null)
                .build();
    }
}
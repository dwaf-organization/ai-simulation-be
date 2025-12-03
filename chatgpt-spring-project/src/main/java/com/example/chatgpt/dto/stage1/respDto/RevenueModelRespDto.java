package com.example.chatgpt.dto.stage1.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.chatgpt.entity.RevenueModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueModelRespDto {
    
    private Integer revenueModelCode;
    private Integer teamCode;
    private Integer eventCode;
    private Integer revenueCategory;
    
    public static RevenueModelRespDto from(RevenueModel revenueModel) {
        return RevenueModelRespDto.builder()
            .revenueModelCode(revenueModel.getRevenueModelCode())
            .teamCode(revenueModel.getTeamCode())
            .eventCode(revenueModel.getEventCode())
            .revenueCategory(revenueModel.getRevenueCategory())
            .build();
    }
}
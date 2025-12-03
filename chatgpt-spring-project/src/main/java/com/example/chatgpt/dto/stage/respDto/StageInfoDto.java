package com.example.chatgpt.dto.stage.respDto;

import com.example.chatgpt.entity.StageMst;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageInfoDto {

    private Integer stageId;
    private Integer stepId;
    private String stepName;
    private String type;

    public static StageInfoDto from(StageMst stageMst) {
        return StageInfoDto.builder()
                .stageId(stageMst.getStageId())
                .stepId(stageMst.getStepId())
                .stepName(stageMst.getStepName())
                .type(stageMst.getType())
                .build();
    }
}

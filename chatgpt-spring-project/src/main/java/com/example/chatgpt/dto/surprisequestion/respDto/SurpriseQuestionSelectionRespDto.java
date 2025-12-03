package com.example.chatgpt.dto.surprisequestion.respDto;

import com.example.chatgpt.entity.SurpriseQuestionSelection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionSelectionRespDto {
    
    private Integer sqSelectionCode;    // 돌발질문 선택 코드
    private Integer sqCode;            // 돌발질문 코드
    private String sqAnswer;           // 선택한 답변
    private String aiFeedback;         // AI 피드백
    
    /**
     * Entity를 DTO로 변환
     */
    public static SurpriseQuestionSelectionRespDto from(SurpriseQuestionSelection selection) {
        return SurpriseQuestionSelectionRespDto.builder()
                .sqSelectionCode(selection.getSqSelectionCode())
                .sqCode(selection.getSqCode())
                .sqAnswer(selection.getSqAnswer())
                .aiFeedback(selection.getAiFeedback())
                .build();
    }
}
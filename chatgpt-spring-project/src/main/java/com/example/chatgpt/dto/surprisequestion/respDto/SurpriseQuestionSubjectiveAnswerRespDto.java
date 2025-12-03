package com.example.chatgpt.dto.surprisequestion.respDto;

import com.example.chatgpt.entity.SurpriseQuestionAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionSubjectiveAnswerRespDto {
    
    private Integer sqAnswerCode;    // 주관식 돌발질문 답변 코드
    private Integer sqSubjCode;      // 주관식 돌발질문 코드
    private String answerText;       // 답변 내용
    private String aiFeedback;       // AI 피드백
    
    /**
     * Entity를 DTO로 변환
     */
    public static SurpriseQuestionSubjectiveAnswerRespDto from(SurpriseQuestionAnswer answer) {
        return SurpriseQuestionSubjectiveAnswerRespDto.builder()
                .sqAnswerCode(answer.getSqAnswerCode())
                .sqSubjCode(answer.getSqSubjCode())
                .answerText(answer.getAnswerText())
                .aiFeedback(answer.getAiFeedback())
                .build();
    }
}
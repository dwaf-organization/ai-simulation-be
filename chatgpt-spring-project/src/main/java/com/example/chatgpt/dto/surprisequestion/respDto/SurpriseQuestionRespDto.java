package com.example.chatgpt.dto.surprisequestion.respDto;

import com.example.chatgpt.entity.SurpriseQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionRespDto {
    
    private Integer sqCode;                 // 돌발질문 코드
    private String cardTitle;               // 이벤트카드 제목
    private String situationDescription;    // 상황 설명
    private String questionText;            // 질문 내용
    private String option1;                 // 선택지 1
    private String option2;                 // 선택지 2
    private String option3;                 // 선택지 3
    
    /**
     * Entity를 DTO로 변환 (정답과 힌트는 제외)
     */
    public static SurpriseQuestionRespDto from(SurpriseQuestion surpriseQuestion) {
        return SurpriseQuestionRespDto.builder()
                .sqCode(surpriseQuestion.getSqCode())
                .cardTitle(surpriseQuestion.getCardTitle())
                .situationDescription(surpriseQuestion.getSituationDescription())
                .questionText(surpriseQuestion.getQuestionText())
                .option1(surpriseQuestion.getOption1())
                .option2(surpriseQuestion.getOption2())
                .option3(surpriseQuestion.getOption3())
                .build();
    }
}
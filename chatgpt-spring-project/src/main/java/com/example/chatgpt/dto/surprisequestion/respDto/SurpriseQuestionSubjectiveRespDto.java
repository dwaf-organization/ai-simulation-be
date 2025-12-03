package com.example.chatgpt.dto.surprisequestion.respDto;

import com.example.chatgpt.entity.SurpriseQuestionSubjective;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionSubjectiveRespDto {
    
    private Integer sqSubjCode;             // 주관식 돌발질문 코드
    private String cardTitle;               // 이벤트카드 제목
    private String situationDescription;    // 상황 설명
    private String questionText;            // 질문 내용
    
    /**
     * Entity를 DTO로 변환
     */
    public static SurpriseQuestionSubjectiveRespDto from(SurpriseQuestionSubjective subjective) {
        return SurpriseQuestionSubjectiveRespDto.builder()
                .sqSubjCode(subjective.getSqSubjCode())
                .cardTitle(subjective.getCardTitle())
                .situationDescription(subjective.getSituationDescription())
                .questionText(subjective.getQuestionText())
                .build();
    }
}
package com.example.chatgpt.dto.surprisequestion.respDto;

import com.example.chatgpt.entity.SurpriseQuestion;
import com.example.chatgpt.entity.SurpriseQuestionSelection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionListDto {
    
    // SurpriseQuestionSelection 테이블 필드들 (날짜 제외)
    private Integer sqSelectionCode;        // 선택 코드
    private Integer sqCode;                 // 돌발질문 코드
    private Integer eventCode;              // 행사 코드
    private Integer teamCode;               // 팀 코드
    private String sqAnswer;                // 선택한 답변
    private String aiFeedback;              // AI 피드백
    
    // SurpriseQuestion 테이블 필드들 (조인)
    private String cardTitle;               // 카드 제목
    private String situationDescription;    // 상황 설명
    private String questionText;            // 질문 내용
    private String option1;                 // 선택지 1
    private String option2;                 // 선택지 2
    private String option3;                 // 선택지 3
    
    /**
     * SurpriseQuestionSelection과 SurpriseQuestion을 조합하여 DTO 생성
     */
    public static SurpriseQuestionListDto from(SurpriseQuestionSelection selection, SurpriseQuestion question) {
        return SurpriseQuestionListDto.builder()
                // Selection 테이블 정보
                .sqSelectionCode(selection.getSqSelectionCode())
                .sqCode(selection.getSqCode())
                .eventCode(selection.getEventCode())
                .teamCode(selection.getTeamCode())
                .sqAnswer(selection.getSqAnswer())
                .aiFeedback(selection.getAiFeedback())
                // Question 테이블 정보
                .cardTitle(question.getCardTitle())
                .situationDescription(question.getSituationDescription())
                .questionText(question.getQuestionText())
                .option1(question.getOption1())
                .option2(question.getOption2())
                .option3(question.getOption3())
                .build();
    }
}
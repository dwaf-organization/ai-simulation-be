package com.example.chatgpt.dto.surprisequestion.respDto;

import com.example.chatgpt.entity.SurpriseQuestionAnswer;
import com.example.chatgpt.entity.SurpriseQuestionSubjective;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionSubjectiveListDto {
    
    // SurpriseQuestionAnswer 테이블 필드들 (날짜 제외)
    private Integer sqAnswerCode;           // 답변 코드
    private Integer sqSubjCode;             // 주관식 돌발질문 코드
    private Integer eventCode;              // 행사 코드
    private Integer teamCode;               // 팀 코드
    private String answerText;              // 답변 내용
    private String aiFeedback;              // AI 피드백
    
    // SurpriseQuestionSubjective 테이블 필드들 (조인)
    private String cardTitle;               // 카드 제목
    private String situationDescription;    // 상황 설명
    private String questionText;            // 질문 내용
    
    /**
     * SurpriseQuestionAnswer와 SurpriseQuestionSubjective을 조합하여 DTO 생성
     */
    public static SurpriseQuestionSubjectiveListDto from(SurpriseQuestionAnswer answer, SurpriseQuestionSubjective question) {
        return SurpriseQuestionSubjectiveListDto.builder()
                // Answer 테이블 정보
                .sqAnswerCode(answer.getSqAnswerCode())
                .sqSubjCode(answer.getSqSubjCode())
                .eventCode(answer.getEventCode())
                .teamCode(answer.getTeamCode())
                .answerText(answer.getAnswerText())
                .aiFeedback(answer.getAiFeedback())
                // Question 테이블 정보
                .cardTitle(question.getCardTitle())
                .situationDescription(question.getSituationDescription())
                .questionText(question.getQuestionText())
                .build();
    }
}
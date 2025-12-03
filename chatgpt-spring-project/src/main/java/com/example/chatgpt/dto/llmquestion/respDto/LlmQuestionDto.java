package com.example.chatgpt.dto.llmquestion.respDto;

import com.example.chatgpt.entity.LlmQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmQuestionDto {
    
    private Integer questionCode;       // 질문 코드
    private String category;            // 카테고리
    private String selectionReason;     // 선택 이유
    private String questionSummary;     // 질문 요약
    private String question;            // 질문 내용
    private String option1;             // 선택지 1
    private String option2;             // 선택지 2
    private String option3;             // 선택지 3
    private String option4;             // 선택지 4
    private String option5;             // 선택지 5
    private String userAnswer;          // 사용자 답변
    
    /**
     * Entity를 DTO로 변환
     */
    public static LlmQuestionDto from(LlmQuestion question) {
        return LlmQuestionDto.builder()
                .questionCode(question.getQuestionCode())
                .category(question.getCategory())
                .selectionReason(question.getSelectionReason())
                .questionSummary(question.getQuestionSummary())
                .question(question.getQuestion())
                .option1(question.getOption1())
                .option2(question.getOption2())
                .option3(question.getOption3())
                .option4(question.getOption4())
                .option5(question.getOption5())
                .userAnswer(question.getUserAnswer())
                .build();
    }
}
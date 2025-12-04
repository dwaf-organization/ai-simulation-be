package com.example.chatgpt.dto.quiz.respDto;

import com.example.chatgpt.entity.QuizQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionDto {
    
    private Integer quizCode;           // 퀴즈 코드
    private String questionText;        // 퀴즈 내용
    private String option1;             // 선택지 1
    private String option2;             // 선택지 2
    private String option3;             // 선택지 3
    private String option4;             // 선택지 4
    private String hintText;            // 힌트
    private Integer answer;             // 정답 (1,2,3,4)
    
    /**
     * Entity를 DTO로 변환 (날짜 필드 제외)
     */
    public static QuizQuestionDto from(QuizQuestion entity) {
        return QuizQuestionDto.builder()
                .quizCode(entity.getQuizCode())
                .questionText(entity.getQuestionText())
                .option1(entity.getOption1())
                .option2(entity.getOption2())
                .option3(entity.getOption3())
                .option4(entity.getOption4())
                .hintText(entity.getHintText())
                .answer(entity.getAnswer())
                .build();
    }
}
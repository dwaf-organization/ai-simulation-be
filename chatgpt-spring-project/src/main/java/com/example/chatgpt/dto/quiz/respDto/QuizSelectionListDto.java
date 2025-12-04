package com.example.chatgpt.dto.quiz.respDto;

import com.example.chatgpt.entity.QuizQuestion;
import com.example.chatgpt.entity.QuizSelection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSelectionListDto {
    
    // QuizSelection 테이블 정보
    private Integer quizSelectionCode;  // 퀴즈 선택 코드
    private Integer quizCode;           // 퀴즈 코드
    private Integer teamCode;           // 팀 코드
    private Integer stageStep;          // 스테이지 단계
    
    // QuizQuestion 테이블 정보 (조인)
    private String questionText;        // 퀴즈 내용
    private String option1;             // 선택지 1
    private String option2;             // 선택지 2
    private String option3;             // 선택지 3
    private String option4;             // 선택지 4
    private String hintText;            // 힌트
    private Integer answer;             // 정답 (1,2,3,4)
    
    /**
     * QuizSelection과 QuizQuestion을 조합하여 DTO 생성
     */
    public static QuizSelectionListDto from(QuizSelection selection, QuizQuestion question) {
        return QuizSelectionListDto.builder()
                // Selection 정보
                .quizSelectionCode(selection.getQuizSelectionCode())
                .quizCode(selection.getQuizCode())
                .teamCode(selection.getTeamCode())
                .stageStep(selection.getStageStep())
                // Question 정보
                .questionText(question.getQuestionText())
                .option1(question.getOption1())
                .option2(question.getOption2())
                .option3(question.getOption3())
                .option4(question.getOption4())
                .hintText(question.getHintText())
                .answer(question.getAnswer())
                .build();
    }
}
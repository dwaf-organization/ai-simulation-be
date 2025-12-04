package com.example.chatgpt.dto.quiz.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSelectionLogReqDto {
    
    @NotNull(message = "퀴즈 코드는 필수입니다")
    private Integer quizCode;           // 퀴즈 코드
    
    @NotNull(message = "팀 코드는 필수입니다")
    private Integer teamCode;           // 팀 코드
    
    @NotNull(message = "스테이지 단계는 필수입니다")
    private Integer stageStep;          // 스테이지 단계
}
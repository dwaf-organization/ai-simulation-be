package com.example.chatgpt.dto.quiz.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSelectionListRespDto {
    
    private List<QuizSelectionListDto> quizSelections;  // 퀴즈 선택 목록
    
    /**
     * 퀴즈 선택 목록으로 DTO 생성
     */
    public static QuizSelectionListRespDto from(List<QuizSelectionListDto> quizSelections) {
        return QuizSelectionListRespDto.builder()
                .quizSelections(quizSelections)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static QuizSelectionListRespDto empty() {
        return QuizSelectionListRespDto.builder()
                .quizSelections(List.of())
                .build();
    }
}
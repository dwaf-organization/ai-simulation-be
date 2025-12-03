package com.example.chatgpt.dto.llmquestion.respDto;

import com.example.chatgpt.entity.LlmQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmQuestionListRespDto {
    
    private List<LlmQuestionDto> questions;  // 질문 목록
    
    /**
     * Entity List를 DTO List로 변환
     */
    public static LlmQuestionListRespDto from(List<LlmQuestion> questionList) {
        List<LlmQuestionDto> questionDtos = questionList.stream()
                .map(LlmQuestionDto::from)
                .collect(Collectors.toList());
        
        return LlmQuestionListRespDto.builder()
                .questions(questionDtos)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static LlmQuestionListRespDto empty() {
        return LlmQuestionListRespDto.builder()
                .questions(List.of())
                .build();
    }
}

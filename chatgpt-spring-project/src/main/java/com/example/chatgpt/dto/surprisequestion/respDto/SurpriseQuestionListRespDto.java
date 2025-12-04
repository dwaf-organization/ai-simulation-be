package com.example.chatgpt.dto.surprisequestion.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionListRespDto {
    
    private List<SurpriseQuestionListDto> surpriseQuestions;  // 객관식 돌발질문 목록
    
    /**
     * 객관식 돌발질문 목록으로 DTO 생성
     */
    public static SurpriseQuestionListRespDto from(List<SurpriseQuestionListDto> surpriseQuestions) {
        return SurpriseQuestionListRespDto.builder()
                .surpriseQuestions(surpriseQuestions)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static SurpriseQuestionListRespDto empty() {
        return SurpriseQuestionListRespDto.builder()
                .surpriseQuestions(List.of())
                .build();
    }
}
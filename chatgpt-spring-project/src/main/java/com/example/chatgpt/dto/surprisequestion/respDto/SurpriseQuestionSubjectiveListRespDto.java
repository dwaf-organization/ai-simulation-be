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
public class SurpriseQuestionSubjectiveListRespDto {
    
    private List<SurpriseQuestionSubjectiveListDto> surpriseQuestions;  // 주관식 돌발질문 목록
    
    /**
     * 주관식 돌발질문 목록으로 DTO 생성
     */
    public static SurpriseQuestionSubjectiveListRespDto from(List<SurpriseQuestionSubjectiveListDto> surpriseQuestions) {
        return SurpriseQuestionSubjectiveListRespDto.builder()
                .surpriseQuestions(surpriseQuestions)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static SurpriseQuestionSubjectiveListRespDto empty() {
        return SurpriseQuestionSubjectiveListRespDto.builder()
                .surpriseQuestions(List.of())
                .build();
    }
}
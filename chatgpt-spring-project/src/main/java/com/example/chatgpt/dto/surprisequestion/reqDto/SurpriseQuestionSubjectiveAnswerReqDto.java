package com.example.chatgpt.dto.surprisequestion.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurpriseQuestionSubjectiveAnswerReqDto {
    
    private Integer sqSubjCode;  // 주관식 돌발질문 코드
    private Integer eventCode;   // 행사 코드
    private Integer teamCode;    // 팀 코드
    private String answerText;   // 주관식 답변 내용
}
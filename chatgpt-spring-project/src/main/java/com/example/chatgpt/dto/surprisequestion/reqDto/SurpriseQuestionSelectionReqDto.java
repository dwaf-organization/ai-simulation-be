package com.example.chatgpt.dto.surprisequestion.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurpriseQuestionSelectionReqDto {
    
    private Integer sqCode;      // 돌발질문 코드
    private Integer eventCode;   // 행사 코드
    private Integer teamCode;    // 팀 코드
    private String sqAnswer;     // 선택한 답변 (option1, option2, option3 중 하나)
}
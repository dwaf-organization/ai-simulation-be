package com.example.chatgpt.dto.stage1.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueModelSelectReqDto {
    
    private Integer eventCode;  // 행사 코드
    private Integer teamCode;   // 팀 코드
}
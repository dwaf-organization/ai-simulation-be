package com.example.chatgpt.dto.team.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLoginReqDto {
    
    @NotBlank(message = "팀 ID는 필수입니다.")
    private String teamId;  // 팀 ID
}
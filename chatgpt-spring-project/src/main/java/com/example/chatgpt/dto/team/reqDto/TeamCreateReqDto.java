package com.example.chatgpt.dto.team.reqDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateReqDto {
    
    @NotNull(message = "행사코드는 필수입니다.")
    private Integer eventCode;
}
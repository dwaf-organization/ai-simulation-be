package com.example.chatgpt.dto.team.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamUpdateReqDto {
    
    @NotNull(message = "이벤트 코드는 필수입니다")
    private Integer eventCode;          // 이벤트 코드
    
    @NotNull(message = "팀 코드는 필수입니다")
    private Integer teamCode;           // 팀 코드
    
    @NotBlank(message = "팀 이름은 필수입니다")
    private String teamName;            // 팀 이름
    
    @NotBlank(message = "팀 리더 이름은 필수입니다")
    private String teamLeaderName;      // 팀 리더 이름
    
    private String teamImageUrl;        // 팀 이미지 URL (선택적)
    
    private List<String> members;       // 팀원 이름 목록 (TeamDtl에 저장)
}
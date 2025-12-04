package com.example.chatgpt.dto.stage1bizplan.respDto;

import com.example.chatgpt.entity.Stage1Bizplan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage1BizplanViewDto {
    
    private Integer stage1Code;         // Stage1 코드
    private Integer eventCode;          // 행사 코드
    private Integer teamCode;           // 팀 코드
    private String bizplanFilePath;     // 사업계획서 파일 경로
    private String bizplanContent;      // 사업계획서 내용 (텍스트)
    
    /**
     * Entity를 DTO로 변환
     */
    public static Stage1BizplanViewDto from(Stage1Bizplan entity) {
        return Stage1BizplanViewDto.builder()
                .stage1Code(entity.getStage1Code())
                .eventCode(entity.getEventCode())
                .teamCode(entity.getTeamCode())
                .bizplanFilePath(entity.getBizplanFilePath())
                .bizplanContent(entity.getBizplanContent())  // Entity 필드명과 매핑
                .build();
    }
}
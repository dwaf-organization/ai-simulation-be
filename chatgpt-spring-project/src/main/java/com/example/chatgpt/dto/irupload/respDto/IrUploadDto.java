package com.example.chatgpt.dto.irupload.respDto;

import com.example.chatgpt.entity.IrUpload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrUploadDto {
    
    private Integer irCode;                 // IR 코드
    private Integer eventCode;              // 행사 코드
    private Integer teamCode;               // 팀 코드
    private String irFilePath;              // IR 파일 경로
    private String irWordFilePath;          // IR 워드 파일 경로
    private String irWordContents;          // IR 워드 파일 내용
    
    /**
     * Entity를 DTO로 변환 (날짜 필드 제외)
     */
    public static IrUploadDto from(IrUpload entity) {
        return IrUploadDto.builder()
                .irCode(entity.getIrCode())
                .eventCode(entity.getEventCode())
                .teamCode(entity.getTeamCode())
                .irFilePath(entity.getIrFilePath())
                .irWordFilePath(entity.getIrWordFilePath())
                .irWordContents(entity.getIrWordContents())
                .build();
    }
}
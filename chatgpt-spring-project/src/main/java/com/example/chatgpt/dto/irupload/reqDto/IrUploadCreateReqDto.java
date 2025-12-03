package com.example.chatgpt.dto.irupload.reqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrUploadCreateReqDto {
    
    private Integer eventCode;
    private Integer teamCode;
    
    // Firebase 파일 경로들
    private String irFilePath;
    private String irWordFilePath;
}
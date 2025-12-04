package com.example.chatgpt.dto.irupload.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrUploadListRespDto {
    
    private List<IrUploadDto> irUploads;  // IR 자료 목록
    
    /**
     * IR 자료 목록으로 DTO 생성
     */
    public static IrUploadListRespDto from(List<IrUploadDto> irUploads) {
        return IrUploadListRespDto.builder()
                .irUploads(irUploads)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static IrUploadListRespDto empty() {
        return IrUploadListRespDto.builder()
                .irUploads(List.of())
                .build();
    }
}
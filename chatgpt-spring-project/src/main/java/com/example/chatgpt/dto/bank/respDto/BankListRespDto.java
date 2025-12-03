package com.example.chatgpt.dto.bank.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankListRespDto {
    
    private Integer bankCode;
    private String bankName;
    
    /**
     * Entity → DTO 변환
     */
    public static BankListRespDto fromEntity(com.example.chatgpt.entity.Bank bank) {
        return BankListRespDto.builder()
                .bankCode(bank.getBankCode())
                .bankName(bank.getBankName())
                .build();
    }
}
package com.example.chatgpt.dto.loaninfo.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInfoListRespDto {
    
    private List<LoanInfoDto> loanInfos;  // 대출정보 목록
    
    /**
     * 대출정보 목록으로 DTO 생성
     */
    public static LoanInfoListRespDto from(List<LoanInfoDto> loanInfos) {
        return LoanInfoListRespDto.builder()
                .loanInfos(loanInfos)
                .build();
    }
    
    /**
     * 빈 목록 생성
     */
    public static LoanInfoListRespDto empty() {
        return LoanInfoListRespDto.builder()
                .loanInfos(List.of())
                .build();
    }
}
package com.example.chatgpt.dto.stage6bizplan.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage6CountryBizPlanRespDto {
    
    private CountryGenerationResult usa;
    private CountryGenerationResult china;
    private CountryGenerationResult japan;
    
    /**
     * 국가별 생성 결과
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountryGenerationResult {
        private boolean success;
        private String message;
        private String errorMessage;
        
        public static CountryGenerationResult success(String message) {
            return CountryGenerationResult.builder()
                    .success(true)
                    .message(message)
                    .build();
        }
        
        public static CountryGenerationResult failure(String errorMessage) {
            return CountryGenerationResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
    
    /**
     * 전체 결과 생성
     */
    public static Stage6CountryBizPlanRespDto create(CountryGenerationResult usa, 
                                                     CountryGenerationResult china, 
                                                     CountryGenerationResult japan) {
        return Stage6CountryBizPlanRespDto.builder()
                .usa(usa)
                .china(china)
                .japan(japan)
                .build();
    }
}
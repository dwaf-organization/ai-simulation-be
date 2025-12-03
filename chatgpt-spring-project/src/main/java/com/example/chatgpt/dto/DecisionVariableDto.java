package com.example.chatgpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 의사결정 변수 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionVariableDto {
    
    /**
     * 변수 코드 (예: C01)
     */
    private String code;
    
    /**
     * 대분류 (예: 인사/조직)
     */
    private String majorCategory;
    
    /**
     * 중분류 (예: 채용)
     */
    private String minorCategory;
    
    /**
     * 의사결정변수명 (예: 신규 인력 채용 인원)
     */
    private String variableName;
    
    /**
     * 매출영향 (예: 간접영향(조직 역량↑))
     */
    private String salesImpact;
    
    /**
     * 예산범위 (예: 1~1,000명)
     */
    private String budgetRange;
    
    /**
     * 영향KPI (예: 조직 규모)
     */
    private String impactKpi;
    
    /**
     * 비고 (예: 핵심 투자)
     */
    private String remarks;
}
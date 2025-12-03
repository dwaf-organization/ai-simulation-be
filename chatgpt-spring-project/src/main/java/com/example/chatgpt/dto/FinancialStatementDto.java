package com.example.chatgpt.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * JPA 없이 사용할 수 있는 재무제표 DTO
 * MyBatis나 JDBC 사용 시 이 클래스 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementDto {
    
    private Integer fsCode;
    private Integer teamCode;
    private Integer stageStep;
    
    // 자산 항목들
    private Integer cashAndDeposits; // 현금및예금
    private Integer tangibleAssets; // 유형자산
    private Integer inventoryAssets; // 재고자산
    private Integer ppeAssets; // 유형자산(PPE)
    private Integer intangibleAssets; // 무형자산
    
    // 부채 항목들
    private Integer accountsPayable; // 매입채무
    private Integer borrowings; // 차입금
    
    // 자본 항목들
    private Integer capitalStock; // 자본금
    private Integer totalAssets; // 자산총계
    private Integer totalLiabilitiesEquity; // 부채및자본총계
    
    // 손익계산서 항목들
    private Integer revenue; // 매출액
    private Integer cogs; // 매출원가
    private Integer grossProfit; // 매출총이익
    private Integer sgnaExpenses; // 판매비와관리비
    private Integer rndExpenses; // 연구개발비
    private Integer operatingIncome; // 영업이익
    private Integer nonOperatingIncome; // 영업외수익
    private Integer corporateTax; // 법인세
    private Integer netIncome; // 순이익
    private Integer fsScore; // 재무점수
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 생성 시간 설정
     */
    public void setCreationTime() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    /**
     * 수정 시간 설정
     */
    public void setUpdateTime() {
        this.updatedAt = LocalDateTime.now();
    }
}
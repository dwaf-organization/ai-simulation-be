package com.example.chatgpt.dto.financialstatement.respDto;

import com.example.chatgpt.entity.FinancialStatement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementDto {
    
    private Integer fsCode;                     // 재무제표 코드
    private Integer eventCode;                  // 이벤트 코드
    private Integer teamCode;                   // 팀 코드
    private Integer stageStep;                  // 스테이지 단계
    
    // 자산 항목들
    private Integer cashAndDeposits;            // 현금및예금
    private Integer tangibleAssets;             // 유형자산
    private Integer inventoryAssets;            // 재고자산
    private Integer ppeAssets;                  // 유형자산(PPE)
    private Integer intangibleAssets;           // 무형자산
    
    // 부채 항목들
    private Integer accountsPayable;            // 매입채무
    private Integer borrowings;                 // 차입금
    
    // 자본 항목들
    private Integer capitalStock;               // 자본금
    private Integer totalAssets;                // 자산총계
    private Integer totalLiabilitiesEquity;     // 부채및자본총계
    
    // 손익계산서 항목들
    private Integer revenue;                    // 매출액
    private Integer cogs;                       // 매출원가
    private Integer grossProfit;                // 매출총이익
    private Integer sgnaExpenses;               // 판매비와관리비
    private Integer rndExpenses;                // 연구개발비
    private Integer operatingIncome;            // 영업이익
    private Integer nonOperatingIncome;         // 영업외수익
    private Integer corporateTax;               // 법인세
    private Integer netIncome;                  // 순이익
    private Integer fsScore;                    // 재무점수
    
    /**
     * Entity를 DTO로 변환 (날짜 필드 제외)
     */
    public static FinancialStatementDto from(FinancialStatement entity) {
        return FinancialStatementDto.builder()
                .fsCode(entity.getFsCode())
                .eventCode(entity.getEventCode())
                .teamCode(entity.getTeamCode())
                .stageStep(entity.getStageStep())
                .cashAndDeposits(entity.getCashAndDeposits())
                .tangibleAssets(entity.getTangibleAssets())
                .inventoryAssets(entity.getInventoryAssets())
                .ppeAssets(entity.getPpeAssets())
                .intangibleAssets(entity.getIntangibleAssets())
                .accountsPayable(entity.getAccountsPayable())
                .borrowings(entity.getBorrowings())
                .capitalStock(entity.getCapitalStock())
                .totalAssets(entity.getTotalAssets())
                .totalLiabilitiesEquity(entity.getTotalLiabilitiesEquity())
                .revenue(entity.getRevenue())
                .cogs(entity.getCogs())
                .grossProfit(entity.getGrossProfit())
                .sgnaExpenses(entity.getSgnaExpenses())
                .rndExpenses(entity.getRndExpenses())
                .operatingIncome(entity.getOperatingIncome())
                .nonOperatingIncome(entity.getNonOperatingIncome())
                .corporateTax(entity.getCorporateTax())
                .netIncome(entity.getNetIncome())
                .fsScore(entity.getFsScore())
                .build();
    }
}
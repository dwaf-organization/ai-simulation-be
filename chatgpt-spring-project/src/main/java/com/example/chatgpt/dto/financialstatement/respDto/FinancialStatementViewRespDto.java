package com.example.chatgpt.dto.financialstatement.respDto;

import com.example.chatgpt.entity.FinancialStatement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementViewRespDto {
    
    private Integer fsCode;
    private Integer eventCode;
    private Integer teamCode;
    private Integer stageStep;
    private Integer cashAndDeposits;
    private Integer tangibleAssets;
    private Integer inventoryAssets;
    private Integer ppeAssets;
    private Integer intangibleAssets;
    private Integer accountsPayable;
    private Integer borrowings;
    private Integer capitalStock;
    private Integer totalAssets;
    private Integer totalLiabilitiesEquity;
    private Integer revenue;
    private Integer cogs;
    private Integer grossProfit;
    private Integer sgnaExpenses;
    private Integer rndExpenses;
    private Integer operatingIncome;
    private Integer nonOperatingIncome;
    private Integer corporateTax;
    private Integer netIncome;
    private Integer fsScore;
    
    /**
     * Entity → DTO 변환
     */
    public static FinancialStatementViewRespDto fromEntity(FinancialStatement entity) {
        return FinancialStatementViewRespDto.builder()
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
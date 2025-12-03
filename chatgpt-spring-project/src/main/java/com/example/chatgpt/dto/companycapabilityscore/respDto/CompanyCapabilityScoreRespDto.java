package com.example.chatgpt.dto.companycapabilityscore.respDto;

import com.example.chatgpt.entity.CompanyCapabilityScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCapabilityScoreRespDto {
    
    private Integer eventCode;                          // 행사 코드
    private Integer teamCode;                           // 팀 코드
    private Integer strategyCapability;                 // 전략역량 점수
    private Integer financeCapability;                  // 재무역량 점수
    private Integer marketCustomerCapability;           // 시장고객역량 점수
    private Integer operationManagementCapability;      // 운영관리역량 점수
    private Integer technologyInnovationCapability;     // 기술혁신역량 점수
    private Integer sustainabilityCapability;           // 지속가능성역량 점수
    private Integer totalCapabilityLevel;               // 총 역량 레벨
    
    /**
     * Entity를 DTO로 변환
     */
    public static CompanyCapabilityScoreRespDto from(CompanyCapabilityScore score) {
        return CompanyCapabilityScoreRespDto.builder()
                .eventCode(score.getEventCode())
                .teamCode(score.getTeamCode())
                .strategyCapability(score.getStrategyCapability())
                .financeCapability(score.getFinanceCapability())
                .marketCustomerCapability(score.getMarketCustomerCapability())
                .operationManagementCapability(score.getOperationManagementCapability())
                .technologyInnovationCapability(score.getTechnologyInnovationCapability())
                .sustainabilityCapability(score.getSustainabilityCapability())
                .totalCapabilityLevel(score.getTotalCapabilityLevel())
                .build();
    }
}
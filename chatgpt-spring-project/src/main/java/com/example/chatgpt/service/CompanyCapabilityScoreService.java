package com.example.chatgpt.service;

import com.example.chatgpt.dto.companycapabilityscore.respDto.CompanyCapabilityScoreRespDto;
import com.example.chatgpt.entity.CompanyCapabilityScore;
import com.example.chatgpt.repository.CompanyCapabilityScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyCapabilityScoreService {
    
    private final CompanyCapabilityScoreRepository companyCapabilityScoreRepository;
    
    /**
     * 회사 역량 점수 조회
     */
    public CompanyCapabilityScoreRespDto getCompanyCapabilityScore(Integer eventCode, Integer teamCode) {
        try {
            log.info("회사 역량 점수 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 역량 점수 조회
            CompanyCapabilityScore score = companyCapabilityScoreRepository.findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("회사 역량 점수 데이터를 찾을 수 없습니다."));
            
            log.info("회사 역량 점수 조회 완료 - 총 역량 레벨: {}, 전략: {}, 재무: {}, 시장: {}, 운영: {}, 기술: {}, 지속가능: {}", 
                     score.getTotalCapabilityLevel(),
                     score.getStrategyCapability(),
                     score.getFinanceCapability(),
                     score.getMarketCustomerCapability(),
                     score.getOperationManagementCapability(),
                     score.getTechnologyInnovationCapability(),
                     score.getSustainabilityCapability());
            
            return CompanyCapabilityScoreRespDto.from(score);
            
        } catch (Exception e) {
            log.error("회사 역량 점수 조회 실패", e);
            throw new RuntimeException("회사 역량 점수 조회 실패: " + e.getMessage());
        }
    }
}
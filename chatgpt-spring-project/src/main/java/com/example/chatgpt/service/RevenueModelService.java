package com.example.chatgpt.service;

import com.example.chatgpt.dto.stage1.reqDto.RevenueModelReqDto;
import com.example.chatgpt.dto.stage1.respDto.RevenueModelSelectRespDto;
import com.example.chatgpt.entity.RevenueModel;
import com.example.chatgpt.entity.Stage1Bizplan;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.repository.RevenueModelRepository;
import com.example.chatgpt.repository.Stage1BizplanRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RevenueModelService {
    
    private final RevenueModelRepository revenueModelRepository;
    private final TeamMstRepository teamMstRepository;
    private final Stage1BizplanRepository stage1BizplanRepository;
    
    /**
     * 수익모델 설정 및 저장
     */
    @Transactional
    public RevenueModel setRevenueModel(RevenueModelReqDto request) {
        log.info("수익모델 설정 시작 - teamCode: {}, revenueCategory: {}", 
                 request.getTeamCode(), request.getRevenueCategory());
        
        try {
            // 1. 팀 유효성 검증
            Optional<TeamMst> optionalTeam = teamMstRepository.findById(request.getTeamCode());
            if (optionalTeam.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 팀입니다.");
            }
            
            TeamMst team = optionalTeam.get();
            Integer eventCode = team.getEventCode();
            
            // 2. 기존 수익모델 확인 (덮어쓰기)
            Optional<RevenueModel> existingModel = revenueModelRepository.findByTeamCode(request.getTeamCode());
            
            RevenueModel revenueModel;
            
            if (existingModel.isPresent()) {
                // 기존 모델 업데이트
                revenueModel = existingModel.get();
                revenueModel.setRevenueCategory(request.getRevenueCategory());
                clearAllFields(revenueModel);  // 기존 필드 초기화
                
                log.info("기존 수익모델 업데이트 - revenueModelCode: {}", revenueModel.getRevenueModelCode());
                
            } else {
                // 새로 생성
                revenueModel = RevenueModel.builder()
                    .teamCode(request.getTeamCode())
                    .eventCode(eventCode)
                    .revenueCategory(request.getRevenueCategory())
                    .build();
                
                log.info("새 수익모델 생성");
            }
            
            // 3. 카테고리별 필드 매핑
            mapFieldsByCategory(revenueModel, request);
            
            // 4. 수익모델 저장
            RevenueModel savedModel = revenueModelRepository.save(revenueModel);
            
            // 5. 수익모델 텍스트 생성
            String revenueModelText = generateRevenueModelText(savedModel);
            
            // 6. stage1_bizplan 테이블의 biz_item_summary 업데이트
            updateBizItemSummary(request.getTeamCode(), eventCode, revenueModelText);
            
            log.info("수익모델 설정 완료 - revenueModelCode: {}, category: {}", 
                     savedModel.getRevenueModelCode(), savedModel.getRevenueCategory());
            
            return savedModel;
            
        } catch (Exception e) {
            log.error("수익모델 설정 실패", e);
            throw new RuntimeException("수익모델 설정 실패: " + e.getMessage());
        }
    }
    
    /**
     * 모든 필드 초기화 (카테고리 변경 시)
     */
    private void clearAllFields(RevenueModel model) {
        // SaaS/구독
        model.setMonthlySubscriptionFee(null);
        model.setProductDevInvestment(null);
        model.setMarketingBudget(null);
        model.setCustomerSupportCost(null);
        model.setFreeTrialPeriod(null);
        
        // 플랫폼/중개
        model.setTransactionFeeRate(null);
        model.setSupplierAcquisitionBudget(null);
        model.setLearnerAcquisitionBudget(null);
        model.setPlatformFeatureInvest(null);
        model.setTrustSafetyInvest(null);
        
        // E-커머스
        model.setAvgSalePrice(null);
        model.setProductCost(null);
        model.setAdMarketingBudget(null);
        model.setSiteAppImproveInvest(null);
        model.setLogisticsInventoryInvest(null);
        
        // 서비스/에이전시
        model.setHourlyChargeRate(null);
        model.setSalesActivityHours(null);
        model.setProjectExecutionHours(null);
        model.setTeamSkillInvest(null);
        model.setInternalRndHours(null);
        
        // 제조/하드웨어
        model.setProductRetailPrice(null);
        model.setQuarterlyProductionGoal(null);
        model.setRndDesignInvest(null);
        model.setProcessImprovementInvest(null);
        model.setSalesDistributionInvest(null);
        
        // 광고
        model.setContentProductionInvest(null);
        model.setTrafficAcquisitionBudget(null);
        model.setAdAgeTargetSettings(null);
        model.setAdDensityPerPage(null);
        model.setUxImprovementInvest(null);
        
        // 하이브리드
        model.setPrimaryRevenueOption(null);
        model.setSecondaryRevenueOption(null);
        model.setConversionRatio(null);
    }
    
    /**
     * 카테고리별 필드 매핑
     */
    private void mapFieldsByCategory(RevenueModel model, RevenueModelReqDto request) {
        switch (request.getRevenueCategory()) {
            case 1: // SaaS/구독
                model.setMonthlySubscriptionFee(request.getMonthlySubscriptionFee());
                model.setProductDevInvestment(request.getProductDevInvestment());
                model.setMarketingBudget(request.getMarketingBudget());
                model.setCustomerSupportCost(request.getCustomerSupportCost());
                model.setFreeTrialPeriod(request.getFreeTrialPeriod());
                break;
                
            case 2: // 플랫폼/중개
                model.setTransactionFeeRate(request.getTransactionFeeRate());
                model.setSupplierAcquisitionBudget(request.getSupplierAcquisitionBudget());
                model.setLearnerAcquisitionBudget(request.getLearnerAcquisitionBudget());
                model.setPlatformFeatureInvest(request.getPlatformFeatureInvest());
                model.setTrustSafetyInvest(request.getTrustSafetyInvest());
                break;
                
            case 3: // E-커머스
                model.setAvgSalePrice(request.getAvgSalePrice());
                model.setProductCost(request.getProductCost());
                model.setAdMarketingBudget(request.getAdMarketingBudget());
                model.setSiteAppImproveInvest(request.getSiteAppImproveInvest());
                model.setLogisticsInventoryInvest(request.getLogisticsInventoryInvest());
                break;
                
            case 4: // 서비스/에이전시
                model.setHourlyChargeRate(request.getHourlyChargeRate());
                model.setSalesActivityHours(request.getSalesActivityHours());
                model.setProjectExecutionHours(request.getProjectExecutionHours());
                model.setTeamSkillInvest(request.getTeamSkillInvest());
                model.setInternalRndHours(request.getInternalRndHours());
                break;
                
            case 5: // 제조/하드웨어
                model.setProductRetailPrice(request.getProductRetailPrice());
                model.setQuarterlyProductionGoal(request.getQuarterlyProductionGoal());
                model.setRndDesignInvest(request.getRndDesignInvest());
                model.setProcessImprovementInvest(request.getProcessImprovementInvest());
                model.setSalesDistributionInvest(request.getSalesDistributionInvest());
                break;
                
            case 6: // 광고
                model.setContentProductionInvest(request.getContentProductionInvest());
                model.setTrafficAcquisitionBudget(request.getTrafficAcquisitionBudget());
                model.setAdAgeTargetSettings(request.getAdAgeTargetSettings());
                model.setAdDensityPerPage(request.getAdDensityPerPage());
                model.setUxImprovementInvest(request.getUxImprovementInvest());
                break;
                
            case 7: // 하이브리드
                model.setPrimaryRevenueOption(request.getPrimaryRevenueOption());
                model.setSecondaryRevenueOption(request.getSecondaryRevenueOption());
                model.setConversionRatio(request.getConversionRatio());
                break;
                
            default:
                throw new IllegalArgumentException("잘못된 수익 카테고리입니다: " + request.getRevenueCategory());
        }
    }
    
    /**
     * 수익모델 텍스트 생성
     */
    private String generateRevenueModelText(RevenueModel model) {
        StringBuilder text = new StringBuilder();
        
        switch (model.getRevenueCategory()) {
            case 1: // SaaS/구독
                text.append("- SaaS/구독 모델\n");
                if (model.getMonthlySubscriptionFee() != null) {
                    text.append("• 월 구독료: ").append(formatMoney(model.getMonthlySubscriptionFee())).append("\n");
                }
                if (model.getProductDevInvestment() != null) {
                    text.append("• 제품 개발 투자: ").append(formatMoney(model.getProductDevInvestment())).append("\n");
                }
                if (model.getMarketingBudget() != null) {
                    text.append("• 마케팅 예산: ").append(formatMoney(model.getMarketingBudget())).append("\n");
                }
                if (model.getCustomerSupportCost() != null) {
                    text.append("• 고객 지원: ").append(formatMoney(model.getCustomerSupportCost())).append("\n");
                }
                if (model.getFreeTrialPeriod() != null) {
                    text.append("• 무료 체험: ").append(model.getFreeTrialPeriod()).append("일\n");
                }
                break;
                
            case 2: // 플랫폼/중개
                text.append("- 플랫폼/중개 모델\n");
                if (model.getTransactionFeeRate() != null) {
                    text.append("• 거래 수수료율: ").append(model.getTransactionFeeRate()).append("%\n");
                }
                if (model.getSupplierAcquisitionBudget() != null) {
                    text.append("• 공급자 유치 마케팅 예산: ").append(model.getSupplierAcquisitionBudget()).append("%\n");
                }
                if (model.getLearnerAcquisitionBudget() != null) {
                    text.append("• 수요자 유치 마케팅 예산: ").append(model.getLearnerAcquisitionBudget()).append("%\n");
                }
                if (model.getPlatformFeatureInvest() != null) {
                    text.append("• 플랫폼 개발 투자: ").append(formatMoney(model.getPlatformFeatureInvest())).append("\n");
                }
                if (model.getTrustSafetyInvest() != null) {
                    text.append("• 신뢰/안전 시스템 투자액: ").append(model.getTrustSafetyInvest()).append("%\n");
                }
                break;
                
            case 3: // E-커머스
                text.append("- E-커머스 모델\n");
                if (model.getAvgSalePrice() != null) {
                    text.append("• 평균 판매가: ").append(formatMoney(model.getAvgSalePrice())).append("\n");
                }
                if (model.getProductCost() != null) {
                    text.append("• 상품 원가: ").append(formatMoney(model.getProductCost())).append("\n");
                }
                if (model.getAdMarketingBudget() != null) {
                    text.append("• 광고/마케팅 예산: ").append(formatMoney(model.getAdMarketingBudget())).append("\n");
                }
                if (model.getSiteAppImproveInvest() != null) {
                    text.append("• 웹사이트/앱 개선 투자액: ").append(formatMoney(model.getSiteAppImproveInvest())).append("\n");
                }
                if (model.getLogisticsInventoryInvest() != null) {
                    text.append("• 물류/재고 시스템 투자액: ").append(formatMoney(model.getLogisticsInventoryInvest())).append("\n");
                }
                break;
                
            case 4: // 서비스/에이전시
                text.append("- 서비스/에이전시 모델\n");
                if (model.getHourlyChargeRate() != null) {
                    text.append("• 시간당 요금: ").append(formatMoney(model.getHourlyChargeRate())).append("\n");
                }
                if (model.getSalesActivityHours() != null) {
                    text.append("• 영업 활동 투입 시간: ").append(model.getSalesActivityHours()).append("시간\n");
                }
                if (model.getProjectExecutionHours() != null) {
                    text.append("• 프로젝트 수행 시간: ").append(model.getProjectExecutionHours()).append("시간\n");
                }
                if (model.getTeamSkillInvest() != null) {
                    text.append("• 팀 역량 강화 투자액: ").append(formatMoney(model.getTeamSkillInvest())).append("\n");
                }
                if (model.getInternalRndHours() != null) {
                    text.append("• 내부 R&D/자동화 시간: ").append(model.getInternalRndHours()).append("시간\n");
                }
                break;
                
            case 5: // 제조/하드웨어
                text.append("- 제조/하드웨어 모델\n");
                if (model.getProductRetailPrice() != null) {
                    text.append("• 제품 소비자가: ").append(formatMoney(model.getProductRetailPrice())).append("\n");
                }
                if (model.getQuarterlyProductionGoal() != null) {
                    text.append("• 분기 생산 목표: ").append(model.getQuarterlyProductionGoal()).append("개\n");
                }
                if (model.getRndDesignInvest() != null) {
                    text.append("• R&D 및 디자인 투자액: ").append(formatMoney(model.getRndDesignInvest())).append("\n");
                }
                if (model.getProcessImprovementInvest() != null) {
                    text.append("• 생산 공정 개선 투자액: ").append(formatMoney(model.getProcessImprovementInvest())).append("\n");
                }
                if (model.getSalesDistributionInvest() != null) {
                    text.append("• 영업/유통 채널 투자액: ").append(formatMoney(model.getSalesDistributionInvest())).append("\n");
                }
                break;
                
            case 6: // 광고
                text.append("- 광고 모델\n");
                if (model.getContentProductionInvest() != null) {
                    text.append("• 콘텐츠 제작 투자액: ").append(formatMoney(model.getContentProductionInvest())).append("\n");
                }
                if (model.getTrafficAcquisitionBudget() != null) {
                    text.append("• 트래픽 확보 마케팅 예산: ").append(formatMoney(model.getTrafficAcquisitionBudget())).append("\n");
                }
                if (model.getAdAgeTargetSettings() != null) {
                    text.append("• 광고 영업팀 시간/투자: ").append(model.getAdAgeTargetSettings()).append("\n");
                }
                if (model.getAdDensityPerPage() != null) {
                    text.append("• 페이지당 광고 밀도: ").append(model.getAdDensityPerPage()).append("\n");
                }
                if (model.getUxImprovementInvest() != null) {
                    text.append("• UX 개선 투자액: ").append(formatMoney(model.getUxImprovementInvest())).append("\n");
                }
                break;
                
            case 7: // 하이브리드
                text.append("- 하이브리드 모델\n");
                if (model.getPrimaryRevenueOption() != null) {
                    text.append("• 주력 수익모델: ").append(model.getPrimaryRevenueOption()).append("\n");
                }
                if (model.getSecondaryRevenueOption() != null) {
                    text.append("• 보조 수익모델: ").append(model.getSecondaryRevenueOption()).append("\n");
                }
                if (model.getConversionRatio() != null) {
                    text.append("• 수익모델 자원 배분율: ").append(model.getConversionRatio()).append("%\n");
                }
                if (model.getMonthlySubscriptionFee() != null) {
                    text.append("• 월 구독료: ").append(formatMoney(model.getMonthlySubscriptionFee())).append("\n");
                }
                if (model.getProductDevInvestment() != null) {
                    text.append("• 제품 개발 투자: ").append(formatMoney(model.getProductDevInvestment())).append("\n");
                }
                if (model.getMarketingBudget() != null) {
                    text.append("• 마케팅 예산: ").append(formatMoney(model.getMarketingBudget())).append("\n");
                }
                if (model.getCustomerSupportCost() != null) {
                    text.append("• 고객 지원: ").append(formatMoney(model.getCustomerSupportCost())).append("\n");
                }
                if (model.getFreeTrialPeriod() != null) {
                    text.append("• 무료 체험: ").append(model.getFreeTrialPeriod()).append("일\n");
                }
                if (model.getTransactionFeeRate() != null) {
                    text.append("• 거래 수수료율: ").append(model.getTransactionFeeRate()).append("%\n");
                }
                if (model.getSupplierAcquisitionBudget() != null) {
                    text.append("• 공급자 유치 마케팅 예산: ").append(model.getSupplierAcquisitionBudget()).append("%\n");
                }
                if (model.getLearnerAcquisitionBudget() != null) {
                    text.append("• 수요자 유치 마케팅 예산: ").append(model.getLearnerAcquisitionBudget()).append("%\n");
                }
                if (model.getPlatformFeatureInvest() != null) {
                    text.append("• 플랫폼 개발 투자: ").append(formatMoney(model.getPlatformFeatureInvest())).append("\n");
                }
                if (model.getTrustSafetyInvest() != null) {
                    text.append("• 신뢰/안전 시스템 투자액: ").append(model.getTrustSafetyInvest()).append("%\n");
                }
                if (model.getAvgSalePrice() != null) {
                    text.append("• 평균 판매가: ").append(formatMoney(model.getAvgSalePrice())).append("\n");
                }
                if (model.getProductCost() != null) {
                    text.append("• 상품 원가: ").append(formatMoney(model.getProductCost())).append("\n");
                }
                if (model.getAdMarketingBudget() != null) {
                    text.append("• 광고/마케팅 예산: ").append(formatMoney(model.getAdMarketingBudget())).append("\n");
                }
                if (model.getSiteAppImproveInvest() != null) {
                    text.append("• 웹사이트/앱 개선 투자액: ").append(formatMoney(model.getSiteAppImproveInvest())).append("\n");
                }
                if (model.getLogisticsInventoryInvest() != null) {
                    text.append("• 물류/재고 시스템 투자액: ").append(formatMoney(model.getLogisticsInventoryInvest())).append("\n");
                }
                if (model.getHourlyChargeRate() != null) {
                    text.append("• 시간당 요금: ").append(formatMoney(model.getHourlyChargeRate())).append("\n");
                }
                if (model.getSalesActivityHours() != null) {
                    text.append("• 영업 활동 투입 시간: ").append(model.getSalesActivityHours()).append("시간\n");
                }
                if (model.getProjectExecutionHours() != null) {
                    text.append("• 프로젝트 수행 시간: ").append(model.getProjectExecutionHours()).append("시간\n");
                }
                if (model.getTeamSkillInvest() != null) {
                    text.append("• 팀 역량 강화 투자액: ").append(formatMoney(model.getTeamSkillInvest())).append("\n");
                }
                if (model.getInternalRndHours() != null) {
                    text.append("• 내부 R&D/자동화 시간: ").append(model.getInternalRndHours()).append("시간\n");
                }
                if (model.getProductRetailPrice() != null) {
                    text.append("• 제품 소비자가: ").append(formatMoney(model.getProductRetailPrice())).append("\n");
                }
                if (model.getQuarterlyProductionGoal() != null) {
                    text.append("• 분기 생산 목표: ").append(model.getQuarterlyProductionGoal()).append("개\n");
                }
                if (model.getRndDesignInvest() != null) {
                    text.append("• R&D 및 디자인 투자액: ").append(formatMoney(model.getRndDesignInvest())).append("\n");
                }
                if (model.getProcessImprovementInvest() != null) {
                    text.append("• 생산 공정 개선 투자액: ").append(formatMoney(model.getProcessImprovementInvest())).append("\n");
                }
                if (model.getSalesDistributionInvest() != null) {
                    text.append("• 영업/유통 채널 투자액: ").append(formatMoney(model.getSalesDistributionInvest())).append("\n");
                }
                if (model.getContentProductionInvest() != null) {
                    text.append("• 콘텐츠 제작 투자액: ").append(formatMoney(model.getContentProductionInvest())).append("\n");
                }
                if (model.getTrafficAcquisitionBudget() != null) {
                    text.append("• 트래픽 확보 마케팅 예산: ").append(formatMoney(model.getTrafficAcquisitionBudget())).append("\n");
                }
                if (model.getAdAgeTargetSettings() != null) {
                    text.append("• 광고 영업팀 시간/투자: ").append(model.getAdAgeTargetSettings()).append("\n");
                }
                if (model.getAdDensityPerPage() != null) {
                    text.append("• 페이지당 광고 밀도: ").append(model.getAdDensityPerPage()).append("\n");
                }
                if (model.getUxImprovementInvest() != null) {
                    text.append("• UX 개선 투자액: ").append(formatMoney(model.getUxImprovementInvest())).append("\n");
                }
                break;
        }
        
        return text.toString();
    }
    
    /**
     * stage1_bizplan의 biz_item_summary 업데이트
     */
    private void updateBizItemSummary(Integer teamCode, Integer eventCode, String revenueModelText) {
        try {
            Optional<Stage1Bizplan> optionalBizplan = stage1BizplanRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (optionalBizplan.isPresent()) {
                Stage1Bizplan bizplan = optionalBizplan.get();
                
                // 기존 사업계획서 내용 + 수익모델 텍스트
                StringBuilder summary = new StringBuilder();
                
                if (bizplan.getBizplanContent() != null) {
                    // 사업계획서 요약 (1000자)
                    String planSummary = bizplan.getBizplanContent().length() > 60000 
                        ? bizplan.getBizplanContent().substring(0, 60000) + "..."
                        : bizplan.getBizplanContent();
                    summary.append("📄 사업계획서 요약\n").append(planSummary).append("\n\n");
                }
                
                // 수익모델 추가
                summary.append("0. 수익모델\n").append(revenueModelText);
                
                bizplan.setBizItemSummary(summary.toString());
                stage1BizplanRepository.save(bizplan);
                
                log.info("biz_item_summary 업데이트 완료 - teamCode: {}, 길이: {}자", teamCode, summary.length());
                
            } else {
                log.warn("사업계획서를 찾을 수 없음 - teamCode: {}, eventCode: {}", teamCode, eventCode);
            }
            
        } catch (Exception e) {
            log.error("biz_item_summary 업데이트 실패", e);
            // 수익모델 저장은 성공이므로 예외 던지지 않음
        }
    }
    
    /**
     * 금액 포맷팅
     */
    private String formatMoney(Integer amount) {
        if (amount == null) return "0원";
        return String.format("%,d원", amount);
    }
    
    /**
     * 수익모델 조회 (null이 아닌 필드만 응답)
     */
    public RevenueModelSelectRespDto getRevenueModel(Integer eventCode, Integer teamCode) {
        try {
            log.info("수익모델 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 수익모델 조회
            Optional<RevenueModel> optionalModel = revenueModelRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (optionalModel.isEmpty()) {
                throw new RuntimeException("수익모델 데이터를 찾을 수 없습니다.");
            }
            
            RevenueModel model = optionalModel.get();
            
            log.info("수익모델 조회 완료 - revenueModelCode: {}, revenueCategory: {}", 
                     model.getRevenueModelCode(), model.getRevenueCategory());
            
            // DTO 변환 (null이 아닌 필드만 포함)
            return RevenueModelSelectRespDto.from(model);
            
        } catch (Exception e) {
            log.error("수익모델 조회 실패", e);
            throw new RuntimeException("수익모델 조회 실패: " + e.getMessage());
        }
    }
}
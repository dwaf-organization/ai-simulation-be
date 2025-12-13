package com.example.chatgpt.service;

import com.example.chatgpt.entity.GroupSummary;
import com.example.chatgpt.repository.GroupSummaryRepository;
import com.example.chatgpt.util.DatabaseLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroupSummaryService {
    
    private final GroupSummaryRepository groupSummaryRepository;
    private final DatabaseLockManager lockManager;  // Lock Manager 추가
    
    /**
     * 그룹 핵심정보 요약 저장/업데이트 (데드락 방지 적용)
     */
    @Transactional
    public GroupSummary saveGroupSummary(
            Integer eventCode, 
            Integer teamCode, 
            Integer stageStep, 
            String businessPlan,
            Map<String, Object> stageAnswers, 
            List<Map<String, Object>> userExpenseInputs) {
        
        log.info("그룹 요약 저장 시작 - eventCode: {}, teamCode: {}, stageStep: {}", 
                 eventCode, teamCode, stageStep);
        
        // 데드락 방지를 위한 Lock 적용
        return lockManager.executeWithLock(DatabaseLockManager.ServiceType.STAGE_SUMMARY, () -> {
            try {
                // 기존 요약 확인
                Optional<GroupSummary> existingSummary = groupSummaryRepository
                    .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
                
                // 답변에서 핵심 정보 추출
                String businessType = extractBusinessType(stageAnswers, businessPlan);
                String coreTechnology = extractCoreTechnology(stageAnswers, businessPlan);
                String revenueModel = extractRevenueModel(stageAnswers, businessPlan);
                String keyAnswers = compressAnswers(stageAnswers);
                String investmentScale = extractInvestmentScale(stageAnswers, userExpenseInputs);
                String strengths = extractStrengths(stageAnswers, businessPlan);
                String weaknesses = extractWeaknesses(stageAnswers, businessPlan);
                String summaryText = generateCompressedSummary(businessPlan, stageAnswers, userExpenseInputs);
                
                GroupSummary groupSummary;
                
                if (existingSummary.isPresent()) {
                    // 업데이트
                    groupSummary = existingSummary.get();
                    groupSummary.setBusinessType(businessType);
                    groupSummary.setCoreTechnology(coreTechnology);
                    groupSummary.setRevenueModel(revenueModel);
                    groupSummary.setKeyAnswers(keyAnswers);
                    groupSummary.setInvestmentScale(investmentScale);
                    groupSummary.setStrengths(strengths);
                    groupSummary.setWeaknesses(weaknesses);
                    groupSummary.setSummaryText(summaryText);
                    
                    log.info("기존 그룹 요약 업데이트 - summaryId: {}", groupSummary.getSummaryId());
                    
                } else {
                    // 새로 생성
                    groupSummary = GroupSummary.builder()
                        .eventCode(eventCode)
                        .teamCode(teamCode)
                        .stageStep(stageStep)
                        .businessType(businessType)
                        .coreTechnology(coreTechnology)
                        .revenueModel(revenueModel)
                        .keyAnswers(keyAnswers)
                        .investmentScale(investmentScale)
                        .strengths(strengths)
                        .weaknesses(weaknesses)
                        .summaryText(summaryText)
                        .build();
                    
                    log.info("새 그룹 요약 생성");
                }
                
                GroupSummary savedSummary = groupSummaryRepository.save(groupSummary);
                
                log.info("그룹 요약 저장 완료 - summaryId: {}, 요약 길이: {}자", 
                         savedSummary.getSummaryId(), 
                         savedSummary.getSummaryText() != null ? savedSummary.getSummaryText().length() : 0);
                
                return savedSummary;
                
            } catch (Exception e) {
                log.error("그룹 요약 저장 실패", e);
                throw new RuntimeException("그룹 요약 저장 실패: " + e.getMessage());
            }
        });
    }
    
    /**
     * 특정 팀의 특정 Stage 요약 조회
     */
    public Optional<GroupSummary> getGroupSummary(Integer eventCode, Integer teamCode, Integer stageStep) {
        try {
            return groupSummaryRepository.findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
        } catch (Exception e) {
            log.error("그룹 요약 조회 실패 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep, e);
            throw new RuntimeException("그룹 요약 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 특정 팀의 모든 Stage 요약 조회
     */
    public List<GroupSummary> getTeamAllSummaries(Integer eventCode, Integer teamCode) {
        try {
            return groupSummaryRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
        } catch (Exception e) {
            log.error("팀 전체 요약 조회 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            throw new RuntimeException("팀 전체 요약 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 사업 유형 추출
     */
    private String extractBusinessType(Map<String, Object> answers, String businessPlan) {
        // 답변과 사업계획서에서 사업 유형 키워드 추출
        String combined = String.join(" ", answers.values().toString(), businessPlan);
        
        if (combined.toLowerCase().contains("ai") || combined.toLowerCase().contains("인공지능")) {
            return "AI/인공지능";
        } else if (combined.toLowerCase().contains("iot") || combined.toLowerCase().contains("사물인터넷")) {
            return "IoT/사물인터넷";
        } else if (combined.toLowerCase().contains("fintech") || combined.toLowerCase().contains("핀테크")) {
            return "핀테크";
        } else if (combined.toLowerCase().contains("헬스") || combined.toLowerCase().contains("의료")) {
            return "헬스케어";
        } else if (combined.toLowerCase().contains("교육") || combined.toLowerCase().contains("에듀테크")) {
            return "에듀테크";
        } else if (combined.toLowerCase().contains("게임")) {
            return "게임";
        } else if (combined.toLowerCase().contains("커머스") || combined.toLowerCase().contains("쇼핑")) {
            return "이커머스";
        } else {
            return "기타";
        }
    }
    
    /**
     * 핵심 기술 추출
     */
    private String extractCoreTechnology(Map<String, Object> answers, String businessPlan) {
        // 기술 관련 키워드 추출 (최대 300자)
        String combined = String.join(" ", answers.values().toString(), businessPlan);
        String tech = combined.length() > 300 ? combined.substring(0, 300) + "..." : combined;
        return tech.trim().isEmpty() ? "미정의" : tech;
    }
    
    /**
     * 수익 모델 추출
     */
    private String extractRevenueModel(Map<String, Object> answers, String businessPlan) {
        // 수익 모델 키워드 추출 (최대 300자)
        String combined = answers.values().toString();
        
        if (combined.contains("구독")) {
            return "구독 모델";
        } else if (combined.contains("광고")) {
            return "광고 모델";
        } else if (combined.contains("수수료")) {
            return "수수료 모델";
        } else if (combined.contains("판매")) {
            return "제품 판매";
        } else if (combined.contains("라이센스")) {
            return "라이센스";
        } else {
            String model = combined.length() > 300 ? combined.substring(0, 300) + "..." : combined;
            return model.trim().isEmpty() ? "미정의" : model;
        }
    }
    
    /**
     * 답변 압축
     */
    private String compressAnswers(Map<String, Object> answers) {
        StringBuilder compressed = new StringBuilder();
        
        for (Map.Entry<String, Object> entry : answers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            
            // 답변을 50자로 압축
            String shortAnswer = value.length() > 50 ? value.substring(0, 50) + "..." : value;
            compressed.append(key).append(": ").append(shortAnswer).append("\n");
        }
        
        return compressed.toString();
    }
    
    /**
     * 투자 규모 추출
     */
    private String extractInvestmentScale(Map<String, Object> answers, List<Map<String, Object>> expenses) {
        if (expenses != null && !expenses.isEmpty()) {
            // 지출 정보에서 투자 규모 계산
            return "예상 투자 필요: " + expenses.size() + "개 항목";
        }
        
        return answers.values().toString().contains("투자") ? "투자 계획 있음" : "투자 계획 미정의";
    }
    
    /**
     * 강점 추출
     */
    private String extractStrengths(Map<String, Object> answers, String businessPlan) {
        // 강점 관련 키워드 추출
        String combined = String.join(" ", answers.values().toString(), businessPlan);
        String strengths = combined.length() > 500 ? combined.substring(0, 500) + "..." : combined;
        return strengths.trim().isEmpty() ? "강점 분석 필요" : strengths;
    }
    
    /**
     * 약점 추출
     */
    private String extractWeaknesses(Map<String, Object> answers, String businessPlan) {
        // 답변에서 위험요소, 제약사항 등 추출
        return "위험 요소 및 제약사항 분석 필요";
    }
    
    /**
     * ChatGPT 저장용 압축 요약 생성
     */
    private String generateCompressedSummary(
            String businessPlan, 
            Map<String, Object> answers, 
            List<Map<String, Object>> expenses) {
        
        StringBuilder summary = new StringBuilder();
        
        // 사업계획서 요약 (200자)
        String planSummary = businessPlan.length() > 200 ? businessPlan.substring(0, 200) + "..." : businessPlan;
        summary.append("사업계획: ").append(planSummary).append("\n");
        
        // 주요 답변 (300자)
        String answersSummary = compressAnswers(answers);
        if (answersSummary.length() > 300) {
            answersSummary = answersSummary.substring(0, 300) + "...";
        }
        summary.append("주요답변: ").append(answersSummary).append("\n");
        
        // 지출 정보 (100자)
        if (expenses != null && !expenses.isEmpty()) {
            summary.append("지출계획: ").append(expenses.size()).append("개 항목");
        }
        
        return summary.toString();
    }
}
package com.example.chatgpt.service;

import com.example.chatgpt.entity.StageSummary;
import com.example.chatgpt.repository.StageSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
public class StageSummaryService {
    
    private final StageSummaryRepository stageSummaryRepository;
    private final OpenAiService openAiService;
    
    /**
     * Stage 요약본 생성
     */
    public StageSummary generateStageSummary(
            Integer eventCode,
            Integer teamCode,
            Integer stageStep,
            String businessPlan,
            Map<String, Object> stageAnswers) {
        
        try {
            log.info("Stage {} 요약본 생성 시작 - teamCode: {}", stageStep, teamCode);
            
            // 1. ChatGPT 프롬프트 생성
            String prompt = buildSummaryPrompt(stageStep, businessPlan, stageAnswers);
            
            // 2. ChatGPT로 요약본 생성
            String summaryContent = openAiService.chat(prompt);
            
            // 3. 기존 요약본 확인 및 업데이트/생성
            StageSummary stageSummary;
            Optional<StageSummary> existing = stageSummaryRepository.findByTeamCodeAndStageStep(teamCode, stageStep);
            
            if (existing.isPresent()) {
                // 기존 요약본 업데이트
                stageSummary = existing.get();
                stageSummary.setSummaryText(summaryContent);
                log.info("기존 Stage {} 요약본 업데이트", stageStep);
            } else {
                // 새 요약본 생성
                stageSummary = StageSummary.builder()
                        .eventCode(eventCode)
                        .teamCode(teamCode)
                        .stageStep(stageStep)
                        .summaryText(summaryContent)
                        .build();
                log.info("새로운 Stage {} 요약본 생성", stageStep);
            }
            
            // 4. 저장
            return stageSummaryRepository.save(stageSummary);
            
        } catch (Exception e) {
            log.error("Stage {} 요약본 생성 실패 - teamCode: {}", stageStep, teamCode, e);
            throw new RuntimeException("요약본 생성 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * ChatGPT 요약본 생성 프롬프트 구축
     */
    private String buildSummaryPrompt(Integer stageStep, String businessPlan, Map<String, Object> stageAnswers) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다음 사업계획서와 Stage ").append(stageStep).append(" 질문-답변을 종합적으로 분석하여 ");
        prompt.append("700-1200자 분량의 상세한 요약본을 작성하세요.\n\n");
        
        // Stage별 맥락 설정
        prompt.append("## Stage ").append(stageStep).append(" 맥락\n");
        prompt.append(getStageContext(stageStep)).append("\n\n");
        
        // 사업계획서
        prompt.append("## 📋 사업계획서\n");
        String businessPlanSummary = businessPlan.length() > 3000 ? 
            businessPlan.substring(0, 3000) + "..." : businessPlan;
        prompt.append(businessPlanSummary).append("\n\n");
        
        // 질문-답변
        prompt.append("## 💬 Stage ").append(stageStep).append(" 질문-답변\n");
        int questionIndex = 1;
        for (Map.Entry<String, Object> entry : stageAnswers.entrySet()) {
            prompt.append("**Q").append(questionIndex).append("**: ").append(entry.getKey()).append("\n");
            prompt.append("**A").append(questionIndex).append("**: ").append(entry.getValue()).append("\n\n");
            questionIndex++;
        }
        
        // 분석 지시사항
        prompt.append("## 📊 분석 요구사항\n");
        prompt.append("다음 구조로 종합적이고 상세한 분석을 제공하세요:\n\n");
        
        prompt.append("### 1. 사업계획서 심층 평가\n");
        prompt.append("- **강점 분석**: 사업 아이디어의 독창성, 시장성, 기술력, 팀 역량 등을 구체적으로 평가\n");
        prompt.append("- **약점 및 리스크**: 잠재적 위험 요소, 경쟁사 대비 취약점, 시장 진입 장벽 등을 솔직하게 지적\n");
        prompt.append("- **차별화 요소**: 경쟁사와 구분되는 핵심 경쟁력과 독특한 가치 제안 분석\n\n");
        
        prompt.append("### 2. Stage ").append(stageStep).append(" 답변 상세 분석\n");
        prompt.append("- **탁월한 선택들**: 각 답변 중 전략적으로 우수하다고 판단되는 결정들과 그 이유\n");
        prompt.append("- **아쉬운 결정들**: 더 나은 대안이 있었을 것으로 보이는 선택들과 개선 방향\n");
        prompt.append("- **일관성 분석**: 답변들 간의 전략적 일치성과 사업 방향성의 명확성 평가\n");
        prompt.append("- **현실성 검토**: 답변의 실현 가능성과 구체성 수준 평가\n\n");
        
        prompt.append("### 3. 핵심 부족 사항 및 개선 포인트\n");
        prompt.append("- **전략적 공백**: 고려되지 않은 중요한 사업 요소들\n");
        prompt.append("- **구체성 부족**: 추상적이거나 모호한 부분들에 대한 구체화 필요성\n");
        prompt.append("- **리스크 관리**: 식별되지 않은 위험 요소나 대응책 부재\n");
        prompt.append("- **자원 배분**: 인력, 자금, 시간 등의 효율적 활용 방안\n\n");
        
        prompt.append("### 4. 향후 사업 추진 방향성\n");
        prompt.append("- **단기 액션 플랜**: 다음 3-6개월 내 집중해야 할 핵심 과제\n");
        prompt.append("- **중장기 전략**: 1-2년 후 목표와 그를 위한 로드맵\n");
        prompt.append("- **우선순위 제시**: 한정된 자원 하에서 가장 먼저 해결해야 할 이슈들\n");
        prompt.append("- **성공 지표**: 진척도를 측정할 수 있는 구체적인 KPI 제안\n\n");
        
        prompt.append("### 5. 종합 결론 및 전망\n");
        prompt.append("- **현재 상태 진단**: 사업의 현재 단계와 성숙도 평가\n");
        prompt.append("- **성공 가능성**: 시장 상황과 팀 역량을 종합한 성공 확률 분석\n");
        prompt.append("- **투자자 관점**: 외부 투자 유치 시 어필 포인트와 우려 사항\n");
        prompt.append("- **최종 권고사항**: 가장 중요한 3가지 핵심 제언\n\n");
        
        // 추가 지시사항
        prompt.append("## ⚠️ 작성 지침\n");
        prompt.append("- **객관성 유지**: 긍정적 측면과 부정적 측면을 균형 있게 다루세요\n");
        prompt.append("- **구체성 중시**: 추상적 표현보다는 실행 가능한 구체적 조언을 제시하세요\n");
        prompt.append("- **전문성 발휘**: 사업 전문가 관점에서 심층적이고 통찰력 있는 분석을 제공하세요\n");
        prompt.append("- **실용성 강조**: 이론적 분석보다는 실무에 바로 적용 가능한 인사이트를 중심으로 하세요\n");
        prompt.append("- **분량 준수**: 700-1200자 내에서 핵심 내용을 압축적으로 전달하세요\n\n");
        
        prompt.append("위의 모든 항목을 포함하여 종합적이고 통찰력 있는 요약본을 작성해주세요.");
        
        return prompt.toString();
    }
    
    /**
     * Stage별 맥락 정보 제공
     */
    private String getStageContext(Integer stageStep) {
        switch (stageStep) {
            case 1:
                return "사업 초기 단계로서 아이디어 검증, 팀 구성, 초기 자금 조달 등 기본 토대를 마련하는 시기입니다. " +
                       "사업의 실현 가능성과 핵심 역량 확보가 주요 관심사입니다.";
            case 2:
                return "초기 운영 단계로서 최소 기능 제품(MVP) 개발, 초기 고객 확보, 운영 프로세스 구축이 중요한 시기입니다. " +
                       "제품-시장 적합성(PMF) 확보와 효율적인 운영 체계 구축이 핵심입니다.";
            case 3:
                return "성장기 운영 단계로서 매출 성장, 시장 점유율 확대, 조직 규모 확장이 주요 과제인 시기입니다. " +
                       "확장 가능한 비즈니스 모델 완성과 지속 가능한 성장 동력 확보가 중요합니다.";
            case 4:
                return "안정화 운영 단계로서 수익성 개선, 운영 효율성 극대화, 경쟁 우위 고도화가 핵심인 시기입니다. " +
                       "안정적인 현금흐름 확보와 시장 내 확고한 포지션 구축이 목표입니다.";
            case 5:
                return "투자 유치 후 단계로서 외부 투자금을 활용한 대규모 성장, 신사업 진출, 기술 혁신이 중요한 시기입니다. " +
                       "투자금의 효율적 활용과 빠른 성장을 통한 가치 증대가 핵심 과제입니다.";
            case 6:
                return "글로벌 진출 단계로서 해외 시장 진출, 다국가 운영, 현지화 전략 수립이 주요 과제인 시기입니다. " +
                       "글로벌 경쟁력 확보와 다양한 시장에서의 성공적인 안착이 중요합니다.";
            case 7:
                return "IPO 준비 단계로서 상장 준비, 기업 지배구조 개선, 투명성 강화가 핵심인 시기입니다. " +
                       "공개 기업으로서의 요건 충족과 지속적인 주주 가치 창출이 최우선 과제입니다.";
            default:
                return "사업 발전의 중요한 단계로서 전략적 의사결정과 실행력이 중요한 시기입니다.";
        }
    }
    
    /**
     * 팀의 모든 Stage 요약본 조회
     */
    public List<StageSummary> getTeamSummaries(Integer teamCode) {
        return stageSummaryRepository.findByTeamCodeOrderByStageStep(teamCode);
    }
    
    /**
     * 특정 Stage 요약본 조회
     */
    public Optional<StageSummary> getStageSummary(Integer teamCode, Integer stageStep) {
        return stageSummaryRepository.findByTeamCodeAndStageStep(teamCode, stageStep);
    }
    
    /**
     * 이벤트 내 모든 팀의 요약본 조회
     */
    public List<StageSummary> getEventSummaries(Integer eventCode) {
        return stageSummaryRepository.findByEventCodeOrderByTeamAndStage(eventCode);
    }
    
    /**
     * 특정 Stage의 모든 팀 요약본 조회
     */
    public List<StageSummary> getStageAllTeamSummaries(Integer stageStep) {
        return stageSummaryRepository.findByStageStepOrderByTeamCode(stageStep);
    }
    
    /**
     * 이벤트 내 특정 Stage의 모든 팀 요약본 조회
     */
    public List<StageSummary> getEventStageSummaries(Integer eventCode, Integer stageStep) {
        return stageSummaryRepository.findByEventCodeAndStageStep(eventCode, stageStep);
    }
    
    /**
     * 요약본 존재 여부 확인
     */
    public boolean existsSummary(Integer teamCode, Integer stageStep) {
        return stageSummaryRepository.existsByTeamCodeAndStageStep(teamCode, stageStep);
    }
    
    /**
     * 요약본 삭제
     */
    public void deleteStageSummary(Integer teamCode, Integer stageStep) {
        stageSummaryRepository.findByTeamCodeAndStageStep(teamCode, stageStep)
            .ifPresent(stageSummaryRepository::delete);
        log.info("Stage {} 요약본 삭제 완료 - teamCode: {}", stageStep, teamCode);
    }
    
    /**
     * 팀의 모든 요약본 삭제
     */
    public void deleteAllTeamSummaries(Integer teamCode) {
        List<StageSummary> summaries = stageSummaryRepository.findByTeamCodeOrderByStageStep(teamCode);
        stageSummaryRepository.deleteAll(summaries);
        log.info("팀 {} 모든 요약본 삭제 완료", teamCode);
    }
}
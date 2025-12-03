//package com.example.chatgpt.service;
//
//import com.example.chatgpt.entity.CompanyCapabilityScore;
//import com.example.chatgpt.repository.CompanyCapabilityScoreRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional
//public class CompanyCapabilityService {
//    
//    private final CompanyCapabilityScoreRepository capabilityRepository;
//    private final OpenAiService openAiService;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    
//    // 6개 역량 항목 정의
//    private static final List<String> CAPABILITY_TYPES = Arrays.asList(
//        "strategy", "financial", "market_customer", 
//        "operation_management", "technology_innovation", "sustainability"
//    );
//    
//    private static final Map<String, String> CAPABILITY_NAMES = Map.of(
//        "strategy", "전략 역량",
//        "financial", "재무 역량", 
//        "market_customer", "시장고객 역량",
//        "operation_management", "운영관리 역량",
//        "technology_innovation", "기술혁신 역량",
//        "sustainability", "지속가능성 역량"
//    );
//    
//    /**
//     * ✅ 핵심: 팀 생성 시 초기 역량 데이터 생성
//     */
//    public CompanyCapabilityScore initializeTeamCapability(Integer eventCode, Integer teamCode) {
//        // 이미 존재하는지 확인
//        if (capabilityRepository.existsByEventCodeAndTeamCode(eventCode, teamCode)) {
//            log.info("팀 {} 역량 데이터가 이미 존재합니다.", teamCode);
//            return getLatestCapabilityScore(eventCode, teamCode);
//        }
//        
//        // Stage 1 기본 데이터 생성
//        CompanyCapabilityScore initialCapability = CompanyCapabilityScore.createDefault(
//            eventCode, teamCode, 1);
//        
//        CompanyCapabilityScore saved = capabilityRepository.save(initialCapability);
//        
//        log.info("팀 {} 초기 역량 데이터 생성 완료", teamCode);
//        return saved;
//    }
//    
//    /**
//     * ✅ 핵심: 스테이지 완료 시 역량 평가 및 점수 부여
//     */
//    public CompanyCapabilityScore evaluateStageCapability(
//            Integer eventCode, 
//            Integer teamCode, 
//            Integer stageStep,
//            String businessPlan,
//            Map<String, Object> stageAnswers,
//            List<Map<String, Object>> userExpenseInputs,
//            String stageSummary) {
//        
//        log.info("팀 {} Stage {} 역량 평가 시작", teamCode, stageStep);
//        
//        try {
//            // 1. 이전 스테이지 점수 조회 또는 생성
//            CompanyCapabilityScore currentCapability = getOrCreateStageCapability(
//                eventCode, teamCode, stageStep);
//            
//            // 2. ChatGPT로 역량 평가 요청
//            Map<String, Object> capabilityEvaluation = requestCapabilityEvaluation(
//                stageStep, businessPlan, stageAnswers, userExpenseInputs, stageSummary);
//            
//            // 3. 평가 결과 적용
//            applyCapabilityScores(currentCapability, capabilityEvaluation);
//            
//            // 4. DB 저장
//            CompanyCapabilityScore savedCapability = capabilityRepository.save(currentCapability);
//            
//            log.info("팀 {} Stage {} 역량 평가 완료 - 총점: {}", 
//                     teamCode, stageStep, savedCapability.getTotalCapabilityLevel());
//            
//            return savedCapability;
//            
//        } catch (Exception e) {
//            log.error("팀 {} Stage {} 역량 평가 실패", teamCode, stageStep, e);
//            
//            // 실패 시 기본 점수 부여 (1점씩)
//            return applyDefaultCapabilityScores(eventCode, teamCode, stageStep);
//        }
//    }
//    
//    /**
//     * ChatGPT로 역량 평가 요청
//     */
//    private Map<String, Object> requestCapabilityEvaluation(
//            Integer stageStep,
//            String businessPlan, 
//            Map<String, Object> stageAnswers,
//            List<Map<String, Object>> userExpenseInputs,
//            String stageSummary) {
//        
//        String prompt = buildCapabilityEvaluationPrompt(
//            stageStep, businessPlan, stageAnswers, userExpenseInputs, stageSummary);
//        
//        String response = openAiService.chat(prompt);
//        
//        return parseCapabilityEvaluationResponse(response);
//    }
//    
//    /**
//     * 역량 평가 프롬프트 생성
//     */
//    private String buildCapabilityEvaluationPrompt(
//            Integer stageStep,
//            String businessPlan,
//            Map<String, Object> stageAnswers, 
//            List<Map<String, Object>> userExpenseInputs,
//            String stageSummary) {
//        
//        StringBuilder prompt = new StringBuilder();
//        
//        prompt.append("다음은 Stage ").append(stageStep).append("을 완료한 팀의 사업 정보입니다.\n\n");
//        
//        // 사업계획서 (축약)
//        prompt.append("**사업계획서 요약:**\n");
//        String shortBusinessPlan = businessPlan.length() > 1000 ? 
//            businessPlan.substring(0, 1000) + "..." : businessPlan;
//        prompt.append(shortBusinessPlan).append("\n\n");
//        
//        // Stage 답변 (상위 5개)
//        prompt.append("**주요 답변:**\n");
//        int count = 0;
//        for (Map.Entry<String, Object> entry : stageAnswers.entrySet()) {
//            if (count >= 5) break;
//            prompt.append("Q: ").append(entry.getKey()).append("\n");
//            prompt.append("A: ").append(entry.getValue()).append("\n\n");
//            count++;
//        }
//        
//        // 지출 정보
//        prompt.append("**지출 계획:**\n");
//        for (Map<String, Object> expense : userExpenseInputs) {
//            prompt.append("- ").append(expense.get("expenseDescription")).append(": ");
//            prompt.append(expense.get("expenseAmount")).append("\n");
//        }
//        
//        // 요약본
//        if (stageSummary != null && !stageSummary.isEmpty()) {
//            prompt.append("\n**전문가 요약:**\n");
//            prompt.append(stageSummary).append("\n");
//        }
//        
//        // 평가 요청
//        prompt.append("\n").append(getCapabilityEvaluationInstructions());
//        
//        return prompt.toString();
//    }
//    
//    /**
//     * 역량 평가 지시사항
//     */
//    private String getCapabilityEvaluationInstructions() {
//        return """
//위 정보를 바탕으로 다음 6개 역량 중에서 가장 두드러지게 향상된 2개 역량을 선정하고, 각각에 1-3점을 부여해주세요.
//
//**6개 역량 항목:**
//1. strategy (전략 역량): 사업 전략, 경쟁 분석, 시장 포지셔닝
//2. financial (재무 역량): 자금 관리, 수익성, 투자 계획
//3. market_customer (시장고객 역량): 고객 이해, 마케팅, 시장 진입
//4. operation_management (운영관리 역량): 운영 효율성, 프로세스, 품질 관리
//5. technology_innovation (기술혁신 역량): 기술 개발, 혁신성, R&D
//6. sustainability (지속가능성 역량): ESG, 사회적 책임, 지속가능한 성장
//
//**점수 기준:**
//- 1점: 기본적인 수준의 향상
//- 2점: 우수한 수준의 향상  
//- 3점: 탁월한 수준의 향상
//
//**응답 형식 (JSON만):**
//{
//  "selectedCapabilities": [
//    {
//      "capability": "strategy",
//      "score": 2,
//      "reason": "시장 분석과 경쟁 전략이 체계적으로 수립되어 전략 역량이 크게 향상됨"
//    },
//    {
//      "capability": "technology_innovation", 
//      "score": 3,
//      "reason": "혁신적인 AI 기술 적용과 차별화된 솔루션으로 기술혁신 역량이 탁월하게 발전됨"
//    }
//  ],
//  "evaluationSummary": "이 팀은 특히 전략 수립과 기술 혁신 분야에서 두드러진 성장을 보였습니다..."
//}
//
//반드시 JSON 형식으로만 응답하세요.
//""";
//    }
//    
//    /**
//     * ChatGPT 응답 파싱
//     */
//    private Map<String, Object> parseCapabilityEvaluationResponse(String response) {
//        try {
//            // JSON 추출
//            String jsonStr = extractJsonFromResponse(response);
//            
//            Map<String, Object> evaluation = objectMapper.readValue(
//                jsonStr, new TypeReference<Map<String, Object>>() {});
//            
//            // 검증
//            if (!evaluation.containsKey("selectedCapabilities")) {
//                throw new RuntimeException("selectedCapabilities 필드가 없습니다.");
//            }
//            
//            @SuppressWarnings("unchecked")
//            List<Map<String, Object>> capabilities = 
//                (List<Map<String, Object>>) evaluation.get("selectedCapabilities");
//            
//            if (capabilities.size() != 2) {
//                throw new RuntimeException("정확히 2개 역량이 선정되어야 합니다.");
//            }
//            
//            return evaluation;
//            
//        } catch (JsonProcessingException e) {
//            log.error("역량 평가 응답 파싱 실패: {}", response);
//            throw new RuntimeException("역량 평가 응답 파싱 실패", e);
//        }
//    }
//    
//    /**
//     * JSON 응답에서 JSON 문자열 추출
//     */
//    private String extractJsonFromResponse(String response) {
//        // JSON 블록 마커 제거
//        String cleaned = response.trim();
//        
//        if (cleaned.startsWith("```json")) {
//            cleaned = cleaned.substring(7);
//        }
//        if (cleaned.startsWith("```")) {
//            cleaned = cleaned.substring(3);
//        }
//        if (cleaned.endsWith("```")) {
//            cleaned = cleaned.substring(0, cleaned.length() - 3);
//        }
//        
//        cleaned = cleaned.trim();
//        
//        // JSON 객체 시작/끝 찾기
//        int startIndex = cleaned.indexOf('{');
//        int endIndex = cleaned.lastIndexOf('}');
//        
//        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
//            return cleaned.substring(startIndex, endIndex + 1);
//        }
//        
//        return cleaned;
//    }
//    
//    /**
//     * 평가 결과를 역량 점수에 적용
//     */
//    private void applyCapabilityScores(
//            CompanyCapabilityScore capability, 
//            Map<String, Object> evaluation) {
//        
//        @SuppressWarnings("unchecked")
//        List<Map<String, Object>> selectedCapabilities = 
//            (List<Map<String, Object>>) evaluation.get("selectedCapabilities");
//        
//        for (Map<String, Object> capabilityScore : selectedCapabilities) {
//            String capabilityType = (String) capabilityScore.get("capability");
//            Integer score = (Integer) capabilityScore.get("score");
//            String reason = (String) capabilityScore.get("reason");
//            
//            // 점수 검증 (1-3점)
//            if (score < 1 || score > 3) {
//                log.warn("잘못된 점수: {}. 기본값 1점 적용", score);
//                score = 1;
//            }
//            
//            // 역량 유형 검증
//            if (!CAPABILITY_TYPES.contains(capabilityType)) {
//                log.warn("알 수 없는 역량 유형: {}. 건너뛰기", capabilityType);
//                continue;
//            }
//            
//            // 점수 적용
//            capability.updateCapabilityScore(capabilityType, score);
//            
//            log.info("역량 점수 적용: {} +{}점 (이유: {})", 
//                     CAPABILITY_NAMES.get(capabilityType), score, reason);
//        }
//    }
//    
//    /**
//     * 기본 역량 점수 적용 (ChatGPT 실패 시)
//     */
//    private CompanyCapabilityScore applyDefaultCapabilityScores(
//            Integer eventCode, Integer teamCode, Integer stageStep) {
//        
//        log.warn("기본 역량 점수 적용 - 팀: {}, Stage: {}", teamCode, stageStep);
//        
//        CompanyCapabilityScore capability = getOrCreateStageCapability(
//            eventCode, teamCode, stageStep);
//        
//        // 무작위로 2개 역량 선택하여 각각 1점 부여
//        List<String> shuffledCapabilities = new ArrayList<>(CAPABILITY_TYPES);
//        Collections.shuffle(shuffledCapabilities);
//        
//        for (int i = 0; i < 2; i++) {
//            capability.updateCapabilityScore(shuffledCapabilities.get(i), 1);
//        }
//        
//        return capabilityRepository.save(capability);
//    }
//    
//    /**
//     * 스테이지별 역량 점수 조회 또는 생성
//     */
//    private CompanyCapabilityScore getOrCreateStageCapability(
//            Integer eventCode, Integer teamCode, Integer stageStep) {
//        
//        // 현재 스테이지 데이터 존재 확인
//        Optional<CompanyCapabilityScore> current = capabilityRepository
//            .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
//        
//        if (current.isPresent()) {
//            return current.get();
//        }
//        
//        // 이전 스테이지 데이터 조회
//        if (stageStep > 1) {
//            Optional<CompanyCapabilityScore> previous = capabilityRepository
//                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep - 1);
//            
//            if (previous.isPresent()) {
//                // 이전 점수 상속
//                CompanyCapabilityScore newCapability = CompanyCapabilityScore
//                    .createFromPrevious(previous.get(), stageStep);
//                return capabilityRepository.save(newCapability);
//            }
//        }
//        
//        // 첫 스테이지이거나 이전 데이터가 없는 경우 기본값 생성
//        CompanyCapabilityScore newCapability = CompanyCapabilityScore
//            .createDefault(eventCode, teamCode, stageStep);
//        return capabilityRepository.save(newCapability);
//    }
//    
//    /**
//     * 팀의 최신 역량 점수 조회
//     */
//    public CompanyCapabilityScore getLatestCapabilityScore(Integer eventCode, Integer teamCode) {
//        List<CompanyCapabilityScore> scores = capabilityRepository
//            .findLatestByEventCodeAndTeamCode(eventCode, teamCode);
//        
//        if (scores.isEmpty()) {
//            // 데이터가 없으면 초기화
//            return initializeTeamCapability(eventCode, teamCode);
//        }
//        
//        return scores.get(0); // 최신 데이터 (ORDER BY stageStep DESC)
//    }
//    
//    /**
//     * 이벤트의 역량 기반 순위 조회
//     */
//    public List<Map<String, Object>> getCapabilityRankings(Integer eventCode) {
//        List<CompanyCapabilityScore> rankings = capabilityRepository
//            .findLatestCapabilityRankings(eventCode);
//        
//        List<Map<String, Object>> result = new ArrayList<>();
//        int rank = 1;
//        
//        for (CompanyCapabilityScore score : rankings) {
//            Map<String, Object> rankData = new HashMap<>();
//            rankData.put("rank", rank++);
//            rankData.put("teamCode", score.getTeamCode());
//            rankData.put("totalCapabilityLevel", score.getTotalCapabilityLevel());
//            rankData.put("stageStep", score.getStageStep());
//            rankData.put("capabilities", formatCapabilities(score));
//            
//            result.add(rankData);
//        }
//        
//        return result;
//    }
//    
//    /**
//     * 역량 점수 포맷팅
//     */
//    private Map<String, Object> formatCapabilities(CompanyCapabilityScore score) {
//        Map<String, Object> capabilities = new HashMap<>();
//        
//        capabilities.put("전략역량", score.getStrategyCapability());
//        capabilities.put("재무역량", score.getFinancialCapability());
//        capabilities.put("시장고객역량", score.getMarketCustomerCapability());
//        capabilities.put("운영관리역량", score.getOperationManagementCapability());
//        capabilities.put("기술혁신역량", score.getTechnologyInnovationCapability());
//        capabilities.put("지속가능성역량", score.getSustainabilityCapability());
//        
//        return capabilities;
//    }
//    
//    /**
//     * 팀의 역량 발전 이력 조회
//     */
//    public List<Map<String, Object>> getTeamCapabilityHistory(Integer eventCode, Integer teamCode) {
//        List<CompanyCapabilityScore> history = capabilityRepository
//            .findByEventCodeAndTeamCodeOrderByStageStep(eventCode, teamCode);
//        
//        return history.stream()
//            .map(score -> {
//                Map<String, Object> stageData = new HashMap<>();
//                stageData.put("stageStep", score.getStageStep());
//                stageData.put("totalLevel", score.getTotalCapabilityLevel());
//                stageData.put("capabilities", formatCapabilities(score));
//                stageData.put("updatedAt", score.getUpdatedAt());
//                return stageData;
//            })
//            .collect(Collectors.toList());
//    }
//}
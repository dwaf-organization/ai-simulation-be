package com.example.chatgpt.service;

import com.example.chatgpt.entity.*;
import com.example.chatgpt.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRevenueDistributionService {
    
    private final OpenAiService openAiService;
    private final GroupSummaryRepository groupSummaryRepository;
    private final TeamRevenueAllocationRepository teamRevenueAllocationRepository;
    private final ChatGptMemoryLogRepository chatGptMemoryLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 1단계: 그룹 핵심 정보 ChatGPT 메모리에 저장
     */
    @Transactional
    public GroupSummary storeGroupSummaryInChatGPT(Integer eventCode, Integer teamCode, Integer stageStep, 
                                                   String businessPlan, Map<String, Object> stageAnswers, 
                                                   List<Map<String, Object>> userExpenseInputs) {
        try {
            log.info("그룹 요약 정보 ChatGPT 저장 시작 - Event: {}, Team: {}, Stage: {}", eventCode, teamCode, stageStep);
            
            // 1. 그룹 핵심 정보 추출 및 압축
            GroupSummary groupSummary = extractGroupSummary(eventCode, teamCode, stageStep, 
                                                           businessPlan, stageAnswers, userExpenseInputs);
            
            // 2. ChatGPT 메모리 저장용 압축 요약 생성
            String compressedSummary = createCompressedSummary(groupSummary, businessPlan, stageAnswers);
            groupSummary.setSummaryText(compressedSummary);
            
            // 3. ChatGPT에 메모리 저장 요청
            String memoryKey = groupSummary.generateMemoryKey();
            String chatGptResponse = storeSummaryInChatGPTMemory(memoryKey, compressedSummary);
            
            // 4. DB 저장
            GroupSummary savedSummary = groupSummaryRepository.save(groupSummary);
            
            // 5. ChatGPT 메모리 로그 저장
            saveChatGPTMemoryLog(eventCode, teamCode, stageStep, memoryKey, compressedSummary, chatGptResponse);
            
            log.info("그룹 요약 정보 ChatGPT 저장 완료 - MemoryKey: {}", memoryKey);
            return savedSummary;
            
        } catch (Exception e) {
            log.error("그룹 요약 정보 ChatGPT 저장 실패", e);
            throw new RuntimeException("ChatGPT 메모리 저장 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 2단계: 관리자 매출 분배 실행
     */
    @Transactional
    public Map<String, Object> executeRevenueDistribution(Integer eventCode, Integer stageStep, String adminUser) {
        try {
            log.info("매출 분배 실행 시작 - Event: {}, Stage: {}, Admin: {}", eventCode, stageStep, adminUser);
            
            // 1. 해당 이벤트-스테이지의 모든 그룹 조회
            List<GroupSummary> groups = groupSummaryRepository.findCompletedSummariesByEventCodeAndStageStep(eventCode, stageStep);
            
            if (groups.size() < 2) {
                throw new IllegalStateException("매출 분배를 위해서는 최소 2개 그룹이 필요합니다. 현재: " + groups.size() + "개");
            }
            
            // 2. ChatGPT에 저장된 메모리 키들 수집
            List<String> memoryKeys = groups.stream()
                    .map(GroupSummary::generateMemoryKey)
                    .collect(Collectors.toList());
            
            // 3. ChatGPT로 매출 분배 분석 요청
            Map<String, Object> distributionResult = requestChatGPTDistribution(eventCode, stageStep, memoryKeys);
            
            // 4. 새 분배 ID 생성
            Integer distributionId = generateNewDistributionId();
            
            // 5. 팀별 매출 분배 결과 저장
//            List<TeamRevenueAllocation> allocations = saveTeamAllocations(distributionId, eventCode, stageStep, distributionResult);
            
            // 6. 각 팀의 매출 분배 완료 로그
//            logTeamAllocations(allocations);
//            
//            Map<String, Object> result = new HashMap<>();
//            result.put("distributionId", distributionId);
//            result.put("totalTeams", groups.size());
//            result.put("allocations", allocations);
//            result.put("distributionLogic", distributionResult.get("distributionLogic"));
            
            log.info("매출 분배 실행 완료 - DistributionId: {}", distributionId);
//            return result;
            return null;
            
        } catch (Exception e) {
            log.error("매출 분배 실행 실패", e);
            throw new RuntimeException("매출 분배 실행 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 팀별 매출 분배 완료 로그
     */
    private void logTeamAllocations(List<TeamRevenueAllocation> allocations) {
        for (TeamRevenueAllocation allocation : allocations) {
            log.info("팀 {} 매출 분배 완료: {}원 ({}위)", 
                    allocation.getTeamCode(), 
                    allocation.getAllocatedRevenue(),
                    allocation.getStageRank());
        }
    }
    
    /**
     * 그룹 핵심 정보 추출
     */
    private GroupSummary extractGroupSummary(Integer eventCode, Integer teamCode, Integer stageStep,
                                           String businessPlan, Map<String, Object> stageAnswers,
                                           List<Map<String, Object>> userExpenseInputs) {
        
        // ChatGPT로 사업계획서에서 핵심 정보 추출
        String extractionPrompt = buildExtractionPrompt(businessPlan, stageAnswers, userExpenseInputs);
        String extractionResult = openAiService.chat(extractionPrompt);
        
        // JSON 파싱하여 GroupSummary 객체 생성
        try {
            String jsonPart = extractJsonFromResponse(extractionResult);
            Map<String, Object> extracted = objectMapper.readValue(jsonPart, Map.class);
            
            return GroupSummary.builder()
                    .eventCode(eventCode)
                    .teamCode(teamCode)
                    .stageStep(stageStep)
                    .businessType((String) extracted.get("businessType"))
                    .coreTechnology((String) extracted.get("coreTechnology"))
                    .revenueModel((String) extracted.get("revenueModel"))
                    .keyAnswers((String) extracted.get("keyAnswers"))
                    .investmentScale((String) extracted.get("investmentScale"))
                    .strengths((String) extracted.get("strengths"))
                    .weaknesses((String) extracted.get("weaknesses"))
                    .build();
                    
        } catch (Exception e) {
            log.warn("ChatGPT 추출 결과 파싱 실패, 기본값 사용", e);
            return createDefaultGroupSummary(eventCode, teamCode, stageStep, businessPlan, stageAnswers);
        }
    }
    
    /**
     * ChatGPT 정보 추출 프롬프트 생성
     */
    private String buildExtractionPrompt(String businessPlan, Map<String, Object> stageAnswers, 
                                       List<Map<String, Object>> userExpenseInputs) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다음 사업계획서와 답변을 분석하여 핵심 정보를 JSON으로 추출해주세요.\n\n");
        
        // 사업계획서 (3000자로 제한)
        String planSummary = businessPlan.length() > 3000 ? businessPlan.substring(0, 3000) + "..." : businessPlan;
        prompt.append("## 사업계획서\n").append(planSummary).append("\n\n");
        
        // 주요 답변
        prompt.append("## 주요 답변\n");
        int answerCount = 1;
        for (Map.Entry<String, Object> entry : stageAnswers.entrySet()) {
            if (answerCount <= 5) { // 상위 5개 답변만
                prompt.append("**Q").append(answerCount).append("**: ").append(entry.getKey()).append("\n");
                prompt.append("**A").append(answerCount).append("**: ").append(entry.getValue()).append("\n\n");
                answerCount++;
            }
        }
        
        // 투자/지출 정보
        if (userExpenseInputs != null && !userExpenseInputs.isEmpty()) {
            prompt.append("## 투자/지출 계획\n");
            for (Map<String, Object> expense : userExpenseInputs) {
                prompt.append("- ").append(expense.get("expenseAmount")).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("## 추출 요구사항\n");
        prompt.append("다음 JSON 형태로 핵심 정보를 추출해주세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"businessType\": \"한 줄로 사업 유형 요약 (예: AI 기반 음식추천 서비스)\",\n");
        prompt.append("  \"coreTechnology\": \"핵심 기술/역량 요약 (예: 머신러닝 추천 알고리즘)\",\n");
        prompt.append("  \"revenueModel\": \"수익 모델 요약 (예: 프리미엄 구독 월 9900원)\",\n");
        prompt.append("  \"keyAnswers\": \"주요 답변 3개 키워드 (예: 지분참여, MVP개발, 소셜마케팅)\",\n");
        prompt.append("  \"investmentScale\": \"투자 규모 요약 (예: 개발자 2명 연봉 1.2억)\",\n");
        prompt.append("  \"strengths\": \"핵심 강점 2-3가지 (예: 개인화 기술, 시장성)\",\n");
        prompt.append("  \"weaknesses\": \"주요 약점/리스크 1-2가지 (예: 초기 데이터 수집 어려움)\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("각 항목은 간결하고 핵심적으로 요약하여 전체 길이가 1000자를 넘지 않도록 해주세요.");
        
        return prompt.toString();
    }
    
    /**
     * ChatGPT 메모리 저장용 압축 요약 생성
     */
    private String createCompressedSummary(GroupSummary groupSummary, String businessPlan, 
                                         Map<String, Object> stageAnswers) {
        StringBuilder compressed = new StringBuilder();
        
        compressed.append("사업유형: ").append(groupSummary.getBusinessType()).append("|");
        compressed.append("핵심기술: ").append(groupSummary.getCoreTechnology()).append("|");
        compressed.append("수익모델: ").append(groupSummary.getRevenueModel()).append("|");
        compressed.append("주요답변: ").append(groupSummary.getKeyAnswers()).append("|");
        compressed.append("투자규모: ").append(groupSummary.getInvestmentScale()).append("|");
        compressed.append("강점: ").append(groupSummary.getStrengths()).append("|");
        compressed.append("약점: ").append(groupSummary.getWeaknesses());
        
        return compressed.toString();
    }
    
    /**
     * ChatGPT 메모리에 요약 저장
     */
    private String storeSummaryInChatGPTMemory(String memoryKey, String compressedSummary) {
        String prompt = String.format(
                "다음 정보를 '%s'라는 키로 기억해주세요. 나중에 매출 분배 분석 시 참고용으로 사용할 예정입니다.\n\n%s\n\n" +
                "이 정보가 정상적으로 저장되었다면 '저장 완료: %s'라고 응답해주세요.",
                memoryKey, compressedSummary, memoryKey
        );
        
        return openAiService.chat(prompt);
    }
    
    /**
     * ChatGPT로 매출 분배 분석 요청
     */
    private Map<String, Object> requestChatGPTDistribution(Integer eventCode, Integer stageStep, List<String> memoryKeys) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("행사").append(eventCode).append(" Stage").append(stageStep).append(" 매출 분배를 진행해주세요.\n\n");
        prompt.append("저장된 그룹 정보들:\n");
        
        for (String memoryKey : memoryKeys) {
            prompt.append("- ").append(memoryKey).append("\n");
        }
        
        prompt.append("\n## 매출 분배 요구사항\n");
        prompt.append("1. 각 그룹의 사업 특성, 기술력, 시장성, 답변 품질을 종합 분석\n");
        prompt.append("2. 월 매출액을 40,000,000~60,000,000원 범위에서 차별화하여 분배\n");
        prompt.append("3. 편차는 ±30% 이내로 하되, 우수한 그룹과 부족한 그룹 명확히 구분\n");
        prompt.append("4. 각 그룹별로 분배 근거 제시 (200자 이내)\n\n");
        
        prompt.append("## 응답 형식\n");
        prompt.append("다음 JSON 형태로 응답해주세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"distributionLogic\": \"전체적인 분배 논리와 기준 설명\",\n");
        prompt.append("  \"teams\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"teamCode\": 1,\n");
        prompt.append("      \"allocatedRevenue\": 52000000,\n");
        prompt.append("      \"reason\": \"AI 기술력과 시장성이 우수하여 가장 높은 매출 배정\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```");
        
        String chatGptResponse = openAiService.chat(prompt.toString());
        
        try {
            // JSON 파싱
            String jsonPart = extractJsonFromResponse(chatGptResponse);
            return objectMapper.readValue(jsonPart, Map.class);
            
        } catch (Exception e) {
            log.error("ChatGPT 매출 분배 결과 파싱 실패", e);
            // 기본 분배 로직으로 폴백
            return createDefaultDistribution(memoryKeys);
        }
    }
    
    /**
     * 팀별 매출 분배 결과 저장
     */
//    private List<TeamRevenueAllocation> saveTeamAllocations(Integer distributionId, Integer eventCode, 
//                                                           Integer stageStep, Map<String, Object> distributionResult) {
//        List<Map<String, Object>> teams = (List<Map<String, Object>>) distributionResult.get("teams");
//        List<TeamRevenueAllocation> allocations = new ArrayList<>();
//        
//        // 매출액 기준으로 순위 계산
//        teams.sort((t1, t2) -> {
//            Long revenue1 = Long.valueOf(t1.get("allocatedRevenue").toString());
//            Long revenue2 = Long.valueOf(t2.get("allocatedRevenue").toString());
//            return revenue2.compareTo(revenue1); // 내림차순
//        });
//        
//        for (int i = 0; i < teams.size(); i++) {
//            Map<String, Object> team = teams.get(i);
//            
//            TeamRevenueAllocation allocation = TeamRevenueAllocation.builder()
//                    .distributionId(distributionId)
//                    .eventCode(eventCode)
//                    .teamCode((Integer) team.get("teamCode"))
//                    .stageStep(stageStep)
//                    .allocatedRevenue(Long.valueOf(team.get("allocatedRevenue").toString()))
//                    .stageRank(i + 1) // 순위 자동 계산
//                    .allocationReason((String) team.get("reason"))
//                    .build();
//            
//            allocations.add(teamRevenueAllocationRepository.save(allocation));
//        }
//        
//        return allocations;
//    }
    
    /**
     * 이벤트-스테이지별 순위 조회
     */
    public List<Map<String, Object>> getEventRankings(Integer eventCode, Integer stageStep) {
        List<TeamRevenueAllocation> allocations = teamRevenueAllocationRepository
                .findByEventCodeAndStageStepOrderByStageRank(eventCode, stageStep);
        
        return allocations.stream().map(this::formatAllocationResponse).collect(Collectors.toList());
    }
    
    /**
     * ChatGPT 메모리 상태 확인
     */
    public Map<String, Object> getChatGPTMemoryStatus(Integer eventCode, Integer stageStep) {
        List<ChatGptMemoryLog> logs = chatGptMemoryLogRepository
                .findHealthyMemoryLogs(eventCode, stageStep, LocalDateTime.now());
        
        Long totalTeams = groupSummaryRepository.countByEventCodeAndStageStep(eventCode, stageStep);
        
        Map<String, Object> status = new HashMap<>();
        status.put("totalTeams", totalTeams);
        status.put("storedMemories", logs.size());
        status.put("missingMemories", totalTeams - logs.size());
        status.put("readyForDistribution", logs.size() >= 2);
        status.put("memoryKeys", logs.stream().map(log -> {
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("memoryKey", log.getMemoryKey());
            keyInfo.put("teamCode", log.getTeamCode());
            keyInfo.put("status", log.getStorageStatus());
            keyInfo.put("storedAt", log.getCreatedAt());
            keyInfo.put("expiresAt", log.getExpiresAt());
            return keyInfo;
        }).collect(Collectors.toList()));
        
        return status;
    }
    
    /**
     * 팀별 매출 분배 이력 조회
     */
    public List<Map<String, Object>> getTeamAllocations(Integer eventCode, Integer teamCode) {
        List<TeamRevenueAllocation> allocations = teamRevenueAllocationRepository
                .findByEventCodeAndTeamCodeOrderByStageStep(eventCode, teamCode);
        
        return allocations.stream().map(this::formatAllocationResponse).collect(Collectors.toList());
    }
    
    /**
     * 관리자 대시보드 데이터
     */
    public Map<String, Object> getAdminDashboard(Integer eventCode) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 기본 정보
        dashboard.put("eventCode", eventCode);
        dashboard.put("lastUpdated", LocalDateTime.now());
        
        // 팀 현황
        List<Object[]> teamStats = teamRevenueAllocationRepository.findTeamRevenueStatistics(eventCode);
        dashboard.put("teamStatistics", formatTeamStatistics(teamStats));
        
        // 메모리 상태
        Long storedMemories = chatGptMemoryLogRepository.countStoredMemoryByEventCode(eventCode);
        dashboard.put("storedMemories", storedMemories);
        
        return dashboard;
    }
    
    // 유틸리티 메서드들
    private String extractJsonFromResponse(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}") + 1;
        if (start >= 0 && end > start) {
            return response.substring(start, end);
        }
        throw new IllegalArgumentException("JSON 형식을 찾을 수 없습니다.");
    }
    
    private GroupSummary createDefaultGroupSummary(Integer eventCode, Integer teamCode, Integer stageStep, 
                                                 String businessPlan, Map<String, Object> stageAnswers) {
        return GroupSummary.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stageStep)
                .businessType("사업계획서 분석 필요")
                .coreTechnology("기술 정보 추출 필요")
                .revenueModel("수익모델 분석 필요")
                .keyAnswers("답변 요약 필요")
                .investmentScale("투자규모 계산 필요")
                .strengths("강점 분석 필요")
                .weaknesses("약점 분석 필요")
                .build();
    }
    
    private Map<String, Object> createDefaultDistribution(List<String> memoryKeys) {
        Map<String, Object> result = new HashMap<>();
        result.put("distributionLogic", "ChatGPT 분석 실패로 기본 분배 로직 적용");
        
        List<Map<String, Object>> teams = new ArrayList<>();
        long baseRevenue = 50000000L; // 기본 5천만원
        
        for (int i = 0; i < memoryKeys.size(); i++) {
            String memoryKey = memoryKeys.get(i);
            Integer teamCode = extractTeamCodeFromMemoryKey(memoryKey);
            
            Map<String, Object> team = new HashMap<>();
            team.put("teamCode", teamCode);
            team.put("allocatedRevenue", baseRevenue + (i % 3 - 1) * 3000000L); // ±300만원 편차
            team.put("reason", "기본 분배 로직 적용");
            
            teams.add(team);
        }
        
        result.put("teams", teams);
        return result;
    }
    
    private Integer extractTeamCodeFromMemoryKey(String memoryKey) {
        // event1_team2_stage3 -> 2 추출
        String[] parts = memoryKey.split("_");
        if (parts.length >= 2) {
            String teamPart = parts[1]; // team2
            return Integer.parseInt(teamPart.replaceAll("[^0-9]", ""));
        }
        return 1; // 기본값
    }
    
    private Integer generateNewDistributionId() {
        return (int) System.currentTimeMillis(); // 간단한 유니크 ID
    }
    
    private void saveChatGPTMemoryLog(Integer eventCode, Integer teamCode, Integer stageStep,
                                    String memoryKey, String content, String response) {
        ChatGptMemoryLog log = ChatGptMemoryLog.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stageStep)
                .memoryKey(memoryKey)
                .storedContent(content)
                .storageStatus(response.toLowerCase().contains("완료") ? "STORED" : "FAILED")
                .chatgptResponse(response)
                .build();
        
        chatGptMemoryLogRepository.save(log);
    }
    
    private Map<String, Object> formatAllocationResponse(TeamRevenueAllocation allocation) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("allocationId", allocation.getAllocationId());
        formatted.put("teamCode", allocation.getTeamCode());
        formatted.put("stageStep", allocation.getStageStep());
        formatted.put("allocatedRevenue", allocation.getAllocatedRevenue());
        formatted.put("formattedRevenue", allocation.getFormattedRevenue());
        formatted.put("stageRank", allocation.getStageRank());
        formatted.put("rankIcon", allocation.getRankIcon());
        formatted.put("allocationReason", allocation.getAllocationReason());
        formatted.put("shortReason", allocation.getShortReason());
        formatted.put("createdAt", allocation.getCreatedAt());
        return formatted;
    }
    
    private List<Map<String, Object>> formatTeamStatistics(List<Object[]> stats) {
        return stats.stream().map(stat -> {
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("teamCode", stat[0]);
            formatted.put("totalRevenue", stat[1]);
            formatted.put("averageRank", stat[2]);
            formatted.put("stageCount", stat[3]);
            return formatted;
        }).collect(Collectors.toList());
    }
}
//package com.example.chatgpt.controller;
//
//import com.example.chatgpt.entity.GroupSummary;
//import com.example.chatgpt.service.AdminRevenueDistributionService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/admin")
//@RequiredArgsConstructor
//@Slf4j
//@CrossOrigin(origins = "*")
//public class AdminRevenueDistributionController {
//    
//    private final AdminRevenueDistributionService adminRevenueDistributionService;
//    
//    /**
//     * 1단계: 그룹 정보를 ChatGPT 메모리에 저장 
//     * 호출시점: 질문 및 답변 완료 후 "완료" 버튼 클릭 시 (자동)
//     * POST /api/admin/store-group-summary
//     */
//    @PostMapping("/store-group-summary")
//    public ResponseEntity<Map<String, Object>> storeGroupSummary(@RequestBody Map<String, Object> request) {
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            Integer eventCode = (Integer) request.get("eventCode");
//            Integer teamCode = (Integer) request.get("teamCode");
//            Integer stageStep = (Integer) request.get("stageStep");
//            String businessPlan = (String) request.get("businessPlan");
//            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
//            List<Map<String, Object>> userExpenseInputs = (List<Map<String, Object>>) request.get("userExpenseInputs");
//            
//            log.info("그룹 요약 정보 ChatGPT 저장 요청 - Event: {}, Team: {}, Stage: {}", eventCode, teamCode, stageStep);
//            
//            GroupSummary groupSummary = adminRevenueDistributionService.storeGroupSummaryInChatGPT(
//                eventCode, teamCode, stageStep, businessPlan, stageAnswers, userExpenseInputs);
//            
//            response.put("success", true);
//            response.put("message", String.format("팀 %d Stage %d 답변이 ChatGPT 메모리에 저장되었습니다.", teamCode, stageStep));
//            response.put("data", formatGroupSummaryResponse(groupSummary));
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("그룹 요약 정보 저장 실패", e);
//            response.put("success", false);
//            response.put("message", "답변 저장 실패: " + e.getMessage());
//            response.put("errorType", "GROUP_SUMMARY_STORE_ERROR");
//            
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 2단계: 요약보기 일괄오픈 + 매출 분배 실행
//     * 호출시점: 관리자가 "요약보기 일괄오픈" 버튼 클릭 시
//     * POST /api/admin/execute-summary-and-distribution
//     */
//    @PostMapping("/execute-summary-and-distribution")
//    public ResponseEntity<Map<String, Object>> executeSummaryAndDistribution(@RequestBody Map<String, Object> request) {
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            Integer eventCode = (Integer) request.get("eventCode");
//            Integer stageStep = (Integer) request.get("stageStep");
//            String adminUser = (String) request.getOrDefault("adminUser", "admin");
//            
//            log.info("요약오픈 + 매출분배 실행 - Event: {}, Stage: {}, Admin: {}", eventCode, stageStep, adminUser);
//            
//            // 1. ChatGPT 메모리 상태 확인
//            Map<String, Object> memoryStatus = adminRevenueDistributionService.getChatGPTMemoryStatus(eventCode, stageStep);
//            Boolean readyForDistribution = (Boolean) memoryStatus.get("readyForDistribution");
//            
//            if (!readyForDistribution) {
//                Integer storedMemories = (Integer) memoryStatus.get("storedMemories");
//                response.put("success", false);
//                response.put("message", String.format("아직 모든 팀의 답변이 준비되지 않았습니다. 현재: %d팀", storedMemories));
//                response.put("errorType", "NOT_READY_FOR_DISTRIBUTION");
//                response.put("memoryStatus", memoryStatus);
//                return ResponseEntity.ok(response);
//            }
//            
//            // 2. 매출 분배 실행
//            Map<String, Object> distributionResult = adminRevenueDistributionService.executeRevenueDistribution(
//                eventCode, stageStep, adminUser);
//            
//            // 3. 순위 조회 (분배 직후)
//            List<Map<String, Object>> rankings = adminRevenueDistributionService.getEventRankings(eventCode, stageStep);
//            
//            // 4. 응답 구성
//            Map<String, Object> summaryData = new HashMap<>();
//            summaryData.put("summaryOpened", true);
//            summaryData.put("distributionCompleted", true);
//            summaryData.put("distributionId", distributionResult.get("distributionId"));
//            summaryData.put("totalTeams", distributionResult.get("totalTeams"));
//            summaryData.put("rankings", rankings);
//            summaryData.put("distributionLogic", distributionResult.get("distributionLogic"));
//            
//            response.put("success", true);
//            response.put("message", String.format("Event %d Stage %d 요약오픈 및 매출분배가 완료되었습니다.", eventCode, stageStep));
//            response.put("data", summaryData);
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("요약오픈 + 매출분배 실행 실패", e);
//            response.put("success", false);
//            response.put("message", "요약오픈 + 매출분배 실패: " + e.getMessage());
//            response.put("errorType", "SUMMARY_DISTRIBUTION_ERROR");
//            
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 현재 순위 조회 (실시간)
//     * 호출시점: 매출 분배 후 자동 호출 또는 순위 페이지 접속 시
//     * GET /api/admin/event/{eventCode}/stage/{stageStep}/rankings
//     */
//    @GetMapping("/event/{eventCode}/stage/{stageStep}/rankings")
//    public ResponseEntity<Map<String, Object>> getEventRankings(
//            @PathVariable Integer eventCode, 
//            @PathVariable Integer stageStep) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            List<Map<String, Object>> rankings = adminRevenueDistributionService.getEventRankings(eventCode, stageStep);
//            
//            response.put("success", true);
//            response.put("data", rankings);
//            response.put("eventCode", eventCode);
//            response.put("stageStep", stageStep);
//            response.put("totalTeams", rankings.size());
//            response.put("message", String.format("Stage %d 순위 조회 완료", stageStep));
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("이벤트 순위 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "순위 조회 실패: " + e.getMessage());
//            response.put("errorType", "RANKING_QUERY_ERROR");
//            
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 관리자 대시보드 - 현재 상태 확인
//     * GET /api/admin/event/{eventCode}/stage/{stageStep}/status
//     */
//    @GetMapping("/event/{eventCode}/stage/{stageStep}/status")
//    public ResponseEntity<Map<String, Object>> getStageStatus(
//            @PathVariable Integer eventCode,
//            @PathVariable Integer stageStep) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            // ChatGPT 메모리 상태
//            Map<String, Object> memoryStatus = adminRevenueDistributionService.getChatGPTMemoryStatus(eventCode, stageStep);
//            
//            // 기존 순위 확인 (이미 분배된 경우)
//            List<Map<String, Object>> existingRankings = adminRevenueDistributionService.getEventRankings(eventCode, stageStep);
//            
//            Map<String, Object> stageStatus = new HashMap<>();
//            stageStatus.put("memoryStatus", memoryStatus);
//            stageStatus.put("hasExistingRankings", !existingRankings.isEmpty());
//            stageStatus.put("existingRankings", existingRankings);
//            stageStatus.put("readyForSummaryOpen", memoryStatus.get("readyForDistribution"));
//            
//            response.put("success", true);
//            response.put("data", stageStatus);
//            response.put("eventCode", eventCode);
//            response.put("stageStep", stageStep);
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("스테이지 상태 확인 실패", e);
//            response.put("success", false);
//            response.put("message", "상태 확인 실패: " + e.getMessage());
//            response.put("errorType", "STAGE_STATUS_ERROR");
//            
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 팀별 매출 분배 이력 조회
//     * GET /api/admin/event/{eventCode}/team/{teamCode}/allocations
//     */
//    @GetMapping("/event/{eventCode}/team/{teamCode}/allocations")
//    public ResponseEntity<Map<String, Object>> getTeamAllocations(
//            @PathVariable Integer eventCode,
//            @PathVariable Integer teamCode) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            List<Map<String, Object>> allocations = adminRevenueDistributionService.getTeamAllocations(eventCode, teamCode);
//            
//            response.put("success", true);
//            response.put("data", allocations);
//            response.put("eventCode", eventCode);
//            response.put("teamCode", teamCode);
//            response.put("totalStages", allocations.size());
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("팀 매출 분배 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "팀 매출 분배 조회 실패: " + e.getMessage());
//            response.put("errorType", "TEAM_ALLOCATION_QUERY_ERROR");
//            
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 전체 이벤트 대시보드
//     * GET /api/admin/dashboard/{eventCode}
//     */
//    @GetMapping("/dashboard/{eventCode}")
//    public ResponseEntity<Map<String, Object>> getAdminDashboard(@PathVariable Integer eventCode) {
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            Map<String, Object> dashboardData = adminRevenueDistributionService.getAdminDashboard(eventCode);
//            
//            // 각 스테이지별 상태 추가
//            Map<String, Object> stageStatuses = new HashMap<>();
//            for (int stage = 1; stage <= 7; stage++) {
//                try {
//                    Map<String, Object> stageStatus = adminRevenueDistributionService.getChatGPTMemoryStatus(eventCode, stage);
//                    List<Map<String, Object>> rankings = adminRevenueDistributionService.getEventRankings(eventCode, stage);
//                    
//                    Map<String, Object> stageInfo = new HashMap<>();
//                    stageInfo.put("memoryStatus", stageStatus);
//                    stageInfo.put("hasRankings", !rankings.isEmpty());
//                    stageInfo.put("rankingCount", rankings.size());
//                    
//                    stageStatuses.put("stage" + stage, stageInfo);
//                } catch (Exception e) {
//                    log.debug("Stage {} 상태 조회 실패: {}", stage, e.getMessage());
//                }
//            }
//            
//            dashboardData.put("stageStatuses", stageStatuses);
//            
//            response.put("success", true);
//            response.put("data", dashboardData);
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("관리자 대시보드 데이터 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "대시보드 데이터 조회 실패: " + e.getMessage());
//            response.put("errorType", "DASHBOARD_ERROR");
//            
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * GroupSummary 응답 포맷
//     */
//    private Map<String, Object> formatGroupSummaryResponse(GroupSummary groupSummary) {
//        Map<String, Object> formatted = new HashMap<>();
//        
//        formatted.put("summaryId", groupSummary.getSummaryId());
//        formatted.put("eventCode", groupSummary.getEventCode());
//        formatted.put("teamCode", groupSummary.getTeamCode());
//        formatted.put("stageStep", groupSummary.getStageStep());
//        formatted.put("businessType", groupSummary.getBusinessType());
//        formatted.put("coreTechnology", groupSummary.getCoreTechnology());
//        formatted.put("revenueModel", groupSummary.getRevenueModel());
//        formatted.put("keyAnswers", groupSummary.getKeyAnswers());
//        formatted.put("investmentScale", groupSummary.getInvestmentScale());
//        formatted.put("strengths", groupSummary.getStrengths());
//        formatted.put("weaknesses", groupSummary.getWeaknesses());
//        formatted.put("memoryKey", groupSummary.generateMemoryKey());
//        formatted.put("isComplete", groupSummary.isComplete());
//        formatted.put("summaryLength", groupSummary.getSummaryText() != null ? groupSummary.getSummaryText().length() : 0);
//        formatted.put("createdAt", groupSummary.getCreatedAt());
//        formatted.put("updatedAt", groupSummary.getUpdatedAt());
//        
//        return formatted;
//    }
//}
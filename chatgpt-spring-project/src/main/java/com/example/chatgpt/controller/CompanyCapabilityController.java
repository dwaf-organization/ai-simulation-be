//package com.example.chatgpt.controller;
//
//import com.example.chatgpt.entity.CompanyCapabilityScore;
//import com.example.chatgpt.service.CompanyCapabilityService;
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
//@RequestMapping("/api/capability")
//@RequiredArgsConstructor
//@Slf4j
//@CrossOrigin(origins = "*")
//public class CompanyCapabilityController {
//    
//    private final CompanyCapabilityService companyCapabilityService;
//    
//    /**
//     * 팀 역량 초기화 (팀 생성 시)
//     * POST /api/capability/initialize
//     */
//    @PostMapping("/initialize")
//    public ResponseEntity<Map<String, Object>> initializeTeamCapability(@RequestBody Map<String, Object> request) {
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            Integer eventCode = (Integer) request.get("eventCode");
//            Integer teamCode = (Integer) request.get("teamCode");
//            
//            log.info("팀 {} 역량 초기화 요청", teamCode);
//            
//            CompanyCapabilityScore capability = companyCapabilityService.initializeTeamCapability(eventCode, teamCode);
//            
//            response.put("success", true);
//            response.put("message", String.format("팀 %d 역량이 초기화되었습니다.", teamCode));
//            response.put("data", formatCapabilityResponse(capability));
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("팀 역량 초기화 실패", e);
//            response.put("success", false);
//            response.put("message", "역량 초기화 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 스테이지 완료 후 역량 평가
//     * POST /api/capability/evaluate-stage
//     */
//    @PostMapping("/evaluate-stage")
//    public ResponseEntity<Map<String, Object>> evaluateStageCapability(@RequestBody Map<String, Object> request) {
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            Integer eventCode = (Integer) request.get("eventCode");
//            Integer teamCode = (Integer) request.get("teamCode");
//            Integer stageStep = (Integer) request.get("stageStep");
//            String businessPlan = (String) request.get("businessPlan");
//            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
//            List<Map<String, Object>> userExpenseInputs = (List<Map<String, Object>>) request.get("userExpenseInputs");
//            String stageSummary = (String) request.get("stageSummary");
//            
//            log.info("팀 {} Stage {} 역량 평가 요청", teamCode, stageStep);
//            
//            CompanyCapabilityScore capability = companyCapabilityService.evaluateStageCapability(
//                eventCode, teamCode, stageStep, businessPlan, stageAnswers, userExpenseInputs, stageSummary);
//            
//            response.put("success", true);
//            response.put("message", String.format("팀 %d Stage %d 역량 평가가 완료되었습니다.", teamCode, stageStep));
//            response.put("data", formatCapabilityResponse(capability));
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("역량 평가 실패", e);
//            response.put("success", false);
//            response.put("message", "역량 평가 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 팀의 현재 역량 점수 조회
//     * GET /api/capability/team/{teamCode}
//     */
//    @GetMapping("/team/{teamCode}")
//    public ResponseEntity<Map<String, Object>> getTeamCapability(
//            @PathVariable Integer teamCode,
//            @RequestParam(defaultValue = "1") Integer eventCode) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            CompanyCapabilityScore capability = companyCapabilityService.getLatestCapabilityScore(eventCode, teamCode);
//            
//            response.put("success", true);
//            response.put("data", formatCapabilityResponse(capability));
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("팀 역량 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "역량 조회 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 팀의 역량 발전 이력 조회
//     * GET /api/capability/team/{teamCode}/history
//     */
//    @GetMapping("/team/{teamCode}/history")
//    public ResponseEntity<Map<String, Object>> getTeamCapabilityHistory(
//            @PathVariable Integer teamCode,
//            @RequestParam(defaultValue = "1") Integer eventCode) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            List<Map<String, Object>> history = companyCapabilityService.getTeamCapabilityHistory(eventCode, teamCode);
//            
//            response.put("success", true);
//            response.put("data", history);
//            response.put("teamCode", teamCode);
//            response.put("totalStages", history.size());
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("팀 역량 이력 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "역량 이력 조회 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 이벤트의 역량 기반 순위 조회
//     * GET /api/capability/rankings
//     */
//    @GetMapping("/rankings")
//    public ResponseEntity<Map<String, Object>> getCapabilityRankings(
//            @RequestParam(defaultValue = "1") Integer eventCode) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            List<Map<String, Object>> rankings = companyCapabilityService.getCapabilityRankings(eventCode);
//            
//            response.put("success", true);
//            response.put("data", rankings);
//            response.put("eventCode", eventCode);
//            response.put("totalTeams", rankings.size());
//            response.put("message", "역량 기반 순위 조회 완료");
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("역량 순위 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "순위 조회 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 특정 스테이지의 역량 순위 조회
//     * GET /api/capability/stage/{stageStep}/rankings
//     */
//    @GetMapping("/stage/{stageStep}/rankings")
//    public ResponseEntity<Map<String, Object>> getStageCapabilityRankings(
//            @PathVariable Integer stageStep,
//            @RequestParam(defaultValue = "1") Integer eventCode) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            // 특정 스테이지 순위는 Repository에서 직접 조회
//            List<Map<String, Object>> rankings = companyCapabilityService.getCapabilityRankings(eventCode);
//            
//            // 해당 스테이지까지 완료한 팀들만 필터링
//            List<Map<String, Object>> filteredRankings = rankings.stream()
//                .filter(rank -> ((Integer) rank.get("stageStep")) >= stageStep)
//                .toList();
//            
//            response.put("success", true);
//            response.put("data", filteredRankings);
//            response.put("eventCode", eventCode);
//            response.put("stageStep", stageStep);
//            response.put("totalTeams", filteredRankings.size());
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("스테이지 역량 순위 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "순위 조회 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 역량별 통계 조회
//     * GET /api/capability/statistics
//     */
//    @GetMapping("/statistics")
//    public ResponseEntity<Map<String, Object>> getCapabilityStatistics(
//            @RequestParam(defaultValue = "1") Integer eventCode) {
//        
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            List<Map<String, Object>> rankings = companyCapabilityService.getCapabilityRankings(eventCode);
//            
//            // 역량별 평균 계산
//            Map<String, Double> capabilityAverages = calculateCapabilityAverages(rankings);
//            
//            // 최고/최저 팀 찾기
//            Map<String, Object> topTeam = rankings.isEmpty() ? null : rankings.get(0);
//            Map<String, Object> bottomTeam = rankings.isEmpty() ? null : rankings.get(rankings.size() - 1);
//            
//            Map<String, Object> statistics = new HashMap<>();
//            statistics.put("totalTeams", rankings.size());
//            statistics.put("capabilityAverages", capabilityAverages);
//            statistics.put("topTeam", topTeam);
//            statistics.put("bottomTeam", bottomTeam);
//            statistics.put("scoreRange", Map.of(
//                "max", topTeam != null ? topTeam.get("totalCapabilityLevel") : 0,
//                "min", bottomTeam != null ? bottomTeam.get("totalCapabilityLevel") : 0
//            ));
//            
//            response.put("success", true);
//            response.put("data", statistics);
//            response.put("eventCode", eventCode);
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            log.error("역량 통계 조회 실패", e);
//            response.put("success", false);
//            response.put("message", "통계 조회 실패: " + e.getMessage());
//            return ResponseEntity.ok(response);
//        }
//    }
//    
//    /**
//     * 역량 점수 응답 포맷팅
//     */
//    private Map<String, Object> formatCapabilityResponse(CompanyCapabilityScore capability) {
//        Map<String, Object> formatted = new HashMap<>();
//        
//        formatted.put("capabilityScoreCode", capability.getCapabilityScoreCode());
//        formatted.put("eventCode", capability.getEventCode());
//        formatted.put("teamCode", capability.getTeamCode());
//        formatted.put("stageStep", capability.getStageStep());
//        formatted.put("totalCapabilityLevel", capability.getTotalCapabilityLevel());
//        
//        // 개별 역량 점수
//        Map<String, Object> capabilities = new HashMap<>();
//        capabilities.put("전략역량", capability.getStrategyCapability());
//        capabilities.put("재무역량", capability.getFinancialCapability());
//        capabilities.put("시장고객역량", capability.getMarketCustomerCapability());
//        capabilities.put("운영관리역량", capability.getOperationManagementCapability());
//        capabilities.put("기술혁신역량", capability.getTechnologyInnovationCapability());
//        capabilities.put("지속가능성역량", capability.getSustainabilityCapability());
//        
//        formatted.put("capabilities", capabilities);
//        formatted.put("createdAt", capability.getCreatedAt());
//        formatted.put("updatedAt", capability.getUpdatedAt());
//        
//        // 역량 레벨 등급
//        formatted.put("capabilityGrade", getCapabilityGrade(capability.getTotalCapabilityLevel()));
//        
//        return formatted;
//    }
//    
//    /**
//     * 역량 등급 계산
//     */
//    private String getCapabilityGrade(Integer totalLevel) {
//        if (totalLevel >= 18) return "S급 (탁월)";
//        if (totalLevel >= 15) return "A급 (우수)";
//        if (totalLevel >= 12) return "B급 (양호)";
//        if (totalLevel >= 9) return "C급 (보통)";
//        if (totalLevel >= 6) return "D급 (미흡)";
//        return "F급 (부족)";
//    }
//    
//    /**
//     * 역량별 평균 계산
//     */
//    private Map<String, Double> calculateCapabilityAverages(List<Map<String, Object>> rankings) {
//        Map<String, Double> averages = new HashMap<>();
//        
//        if (rankings.isEmpty()) {
//            return averages;
//        }
//        
//        Map<String, Integer> totals = new HashMap<>();
//        String[] capabilityKeys = {"전략역량", "재무역량", "시장고객역량", "운영관리역량", "기술혁신역량", "지속가능성역량"};
//        
//        // 합계 계산
//        for (Map<String, Object> rank : rankings) {
//            @SuppressWarnings("unchecked")
//            Map<String, Object> capabilities = (Map<String, Object>) rank.get("capabilities");
//            
//            for (String key : capabilityKeys) {
//                Integer value = (Integer) capabilities.getOrDefault(key, 0);
//                totals.put(key, totals.getOrDefault(key, 0) + value);
//            }
//        }
//        
//        // 평균 계산
//        int teamCount = rankings.size();
//        for (String key : capabilityKeys) {
//            averages.put(key, Math.round(totals.getOrDefault(key, 0) / (double) teamCount * 100.0) / 100.0);
//        }
//        
//        return averages;
//    }
//}
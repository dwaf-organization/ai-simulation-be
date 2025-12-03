package com.example.chatgpt.controller;

import com.example.chatgpt.entity.StageSummary;
import com.example.chatgpt.service.StageSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StageSummaryController {
    
    private final StageSummaryService stageSummaryService;
    
    /**
     * Stage 요약본 생성
     */
    @PostMapping("/stage/{stage}/generate")
    public ResponseEntity<Map<String, Object>> generateStageSummary(
            @PathVariable("stage") Integer stage,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 요청 데이터 추출
            Integer eventCode = (Integer) request.get("eventCode");
            Integer teamCode = (Integer) request.get("teamCode");
            String businessPlan = (String) request.get("businessPlan");
            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
            
            log.info("Stage {} 요약본 생성 요청 - teamCode: {}", stage, teamCode);
            
            // Validation
            if (eventCode == null || teamCode == null || businessPlan == null || stageAnswers == null) {
                response.put("success", false);
                response.put("message", "필수 필드가 누락되었습니다.");
                response.put("errorType", "VALIDATION_ERROR");
                return ResponseEntity.ok(response);
            }
            
            // 요약본 생성
            StageSummary stageSummary = stageSummaryService.generateStageSummary(
                eventCode, teamCode, stage, businessPlan, stageAnswers);
            
            response.put("success", true);
            response.put("message", "Stage " + stage + " 요약본 생성 완료");
            response.put("data", formatSummaryResponse(stageSummary));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage {} 요약본 생성 실패", stage, e);
            response.put("success", false);
            response.put("message", "요약본 생성 중 오류가 발생했습니다: " + e.getMessage());
            response.put("errorType", "SUMMARY_GENERATION_ERROR");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 팀의 모든 Stage 요약본 조회
     */
    @GetMapping("/team/{teamCode}")
    public ResponseEntity<Map<String, Object>> getTeamSummaries(@PathVariable("teamCode") Integer teamCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<StageSummary> summaries = stageSummaryService.getTeamSummaries(teamCode);
            
            response.put("success", true);
            response.put("data", summaries.stream()
                .map(this::formatSummaryResponse)
                .collect(Collectors.toList()));
            response.put("totalCount", summaries.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("팀 {} 요약본 조회 실패", teamCode, e);
            response.put("success", false);
            response.put("message", "요약본 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 특정 Stage 요약본 조회
     */
    @GetMapping("/team/{teamCode}/stage/{stage}")
    public ResponseEntity<Map<String, Object>> getStageSummary(
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stage") Integer stage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            var summary = stageSummaryService.getStageSummary(teamCode, stage);
            
            if (summary.isPresent()) {
                response.put("success", true);
                response.put("data", formatSummaryResponse(summary.get()));
            } else {
                response.put("success", false);
                response.put("message", "해당 Stage의 요약본을 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage {} 요약본 조회 실패 - teamCode: {}", stage, teamCode, e);
            response.put("success", false);
            response.put("message", "요약본 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 이벤트 내 모든 팀의 요약본 조회
     */
    @GetMapping("/event/{eventCode}")
    public ResponseEntity<Map<String, Object>> getEventSummaries(@PathVariable("eventCode") Integer eventCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<StageSummary> summaries = stageSummaryService.getEventSummaries(eventCode);
            
            // 팀별로 그룹화
            Map<Integer, List<StageSummary>> teamGroupedSummaries = summaries.stream()
                .collect(Collectors.groupingBy(StageSummary::getTeamCode));
            
            Map<String, Object> data = new HashMap<>();
            data.put("summariesByTeam", teamGroupedSummaries.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> "team_" + entry.getKey(),
                    entry -> entry.getValue().stream()
                        .map(this::formatSummaryResponse)
                        .collect(Collectors.toList())
                )));
            data.put("totalTeams", teamGroupedSummaries.size());
            data.put("totalSummaries", summaries.size());
            
            response.put("success", true);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("이벤트 {} 요약본 조회 실패", eventCode, e);
            response.put("success", false);
            response.put("message", "이벤트 요약본 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 특정 Stage의 모든 팀 요약본 조회
     */
    @GetMapping("/stage/{stage}/teams")
    public ResponseEntity<Map<String, Object>> getStageAllTeamSummaries(@PathVariable("stage") Integer stage) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<StageSummary> summaries = stageSummaryService.getStageAllTeamSummaries(stage);
            
            response.put("success", true);
            response.put("data", summaries.stream()
                .map(this::formatSummaryResponse)
                .collect(Collectors.toList()));
            response.put("totalTeams", summaries.size());
            response.put("stageStep", stage);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage {} 전체 팀 요약본 조회 실패", stage, e);
            response.put("success", false);
            response.put("message", "Stage 전체 요약본 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 이벤트 내 특정 Stage의 모든 팀 요약본 조회
     */
    @GetMapping("/event/{eventCode}/stage/{stage}")
    public ResponseEntity<Map<String, Object>> getEventStageSummaries(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("stage") Integer stage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<StageSummary> summaries = stageSummaryService.getEventStageSummaries(eventCode, stage);
            
            response.put("success", true);
            response.put("data", summaries.stream()
                .map(this::formatSummaryResponse)
                .collect(Collectors.toList()));
            response.put("eventCode", eventCode);
            response.put("stageStep", stage);
            response.put("totalTeams", summaries.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("이벤트 {} Stage {} 요약본 조회 실패", eventCode, stage, e);
            response.put("success", false);
            response.put("message", "이벤트 Stage 요약본 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 요약본 존재 여부 확인
     */
    @GetMapping("/team/{teamCode}/stage/{stage}/exists")
    public ResponseEntity<Map<String, Object>> checkSummaryExists(
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stage") Integer stage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean exists = stageSummaryService.existsSummary(teamCode, stage);
            
            response.put("success", true);
            response.put("exists", exists);
            response.put("teamCode", teamCode);
            response.put("stageStep", stage);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("요약본 존재 확인 실패 - teamCode: {}, stage: {}", teamCode, stage, e);
            response.put("success", false);
            response.put("message", "요약본 존재 확인 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 요약본 삭제
     */
    @DeleteMapping("/team/{teamCode}/stage/{stage}")
    public ResponseEntity<Map<String, Object>> deleteStageSummary(
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stage") Integer stage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            stageSummaryService.deleteStageSummary(teamCode, stage);
            
            response.put("success", true);
            response.put("message", "Stage " + stage + " 요약본이 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("요약본 삭제 실패 - teamCode: {}, stage: {}", teamCode, stage, e);
            response.put("success", false);
            response.put("message", "요약본 삭제 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 팀의 모든 요약본 삭제
     */
    @DeleteMapping("/team/{teamCode}/all")
    public ResponseEntity<Map<String, Object>> deleteAllTeamSummaries(@PathVariable("teamCode") Integer teamCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            stageSummaryService.deleteAllTeamSummaries(teamCode);
            
            response.put("success", true);
            response.put("message", "팀 " + teamCode + "의 모든 요약본이 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("팀 {} 전체 요약본 삭제 실패", teamCode, e);
            response.put("success", false);
            response.put("message", "팀 요약본 삭제 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 요약본 응답 포맷 정리
     */
    private Map<String, Object> formatSummaryResponse(StageSummary summary) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("summaryCode", summary.getSummaryCode());
        formatted.put("eventCode", summary.getEventCode());
        formatted.put("teamCode", summary.getTeamCode());
        formatted.put("stageStep", summary.getStageStep());
        formatted.put("summaryText", summary.getSummaryText());
        formatted.put("textLength", summary.getSummaryText() != null ? summary.getSummaryText().length() : 0);
        formatted.put("createdAt", summary.getCreatedAt());
        formatted.put("updatedAt", summary.getUpdatedAt());
        
        // 요약본 미리보기 (첫 100자)
        if (summary.getSummaryText() != null && summary.getSummaryText().length() > 100) {
            formatted.put("preview", summary.getSummaryText().substring(0, 100) + "...");
        } else {
            formatted.put("preview", summary.getSummaryText());
        }
        
        return formatted;
    }
}
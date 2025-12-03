package com.example.chatgpt.controller;

import com.example.chatgpt.service.StageSummaryGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StageSummaryGeneratorController {

    private final StageSummaryGeneratorService stageSummaryGeneratorService;

    /**
     * 스테이지 완료 시 요약 생성 API
     */
    @PostMapping("/api/stage/summary/generate/{stage}")
    public ResponseEntity<Map<String, Object>> generateStageSummary(
            @PathVariable("stage") int stage,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer eventCode = (Integer) request.get("eventCode");
            Integer teamCode = (Integer) request.get("teamCode");
            
            if (eventCode == null || teamCode == null) {
                response.put("success", false);
                response.put("message", "eventCode와 teamCode는 필수입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) request.get("answers");
            
            if (answers == null || answers.isEmpty()) {
                response.put("success", false);
                response.put("message", "답변 데이터가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("스테이지 {} 요약 생성 요청 - eventCode: {}, teamCode: {}, 답변 수: {}", 
                stage, eventCode, teamCode, answers.size());
            
            // 답변 저장 및 요약 생성
            Map<String, Object> result = stageSummaryGeneratorService.generateStageSummary(
                eventCode, teamCode, stage, answers);
            
            response.put("success", true);
            response.put("stage", stage);
            response.put("teamCode", teamCode);
            response.put("eventCode", eventCode);
            response.put("data", result);
            
            log.info("스테이지 {} 요약 생성 완료 - teamCode: {}", stage, teamCode);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("스테이지 {} 요약 생성 중 오류 발생", stage, e);
            
            String errorMessage = e.getMessage();
            
            if (errorMessage != null && errorMessage.contains("429")) {
                response.put("success", false);
                response.put("message", "⏰ OpenAI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
                response.put("hint", "무료 계정은 분당 3회 제한이 있습니다.");
                response.put("errorType", "RATE_LIMIT");
                return ResponseEntity.status(429).body(response);
            }
            
            response.put("success", false);
            response.put("message", "요약 생성 중 오류가 발생했습니다: " + errorMessage);
            return ResponseEntity.internalServerError().body(response);
            
        } catch (Exception e) {
            log.error("스테이지 {} 요약 생성 중 예상치 못한 오류 발생", stage, e);
            response.put("success", false);
            response.put("message", "요약 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
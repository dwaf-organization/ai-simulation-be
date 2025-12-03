package com.example.chatgpt.controller;

import com.example.chatgpt.service.GlobalBusinessPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 국가별 사업계획서 생성 컨트롤러
 */
@RestController
@RequestMapping("/api/generate")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GlobalBusinessPlanController {

    private final GlobalBusinessPlanService globalBusinessPlanService;

    /**
     * 국가별 사업계획서 생성
     * 
     * @param country 국가 코드 (USA, CHINA, JAPAN)
     * @param request 요청 본문 (originalText, stageAnswers)
     * @return 생성된 사업계획서
     */
    @PostMapping("/global-business-plan/{country}")
    public ResponseEntity<Map<String, Object>> generateGlobalBusinessPlan(
            @PathVariable("country") String country,
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("국가별 사업계획서 생성 요청: {}", country);
            
            String originalText = (String) request.get("originalText");
            @SuppressWarnings("unchecked")
            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
            
            if (originalText == null || originalText.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "원본 사업계획서가 필요합니다."
                ));
            }
            
            // 국가별 사업계획서 생성
            String businessPlan = globalBusinessPlanService.generateGlobalBusinessPlan(
                country.toUpperCase(), 
                originalText, 
                stageAnswers
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "country", country,
                    "content", businessPlan
                ),
                "message", country + " 형식 사업계획서 생성 완료"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("지원하지 않는 국가: {}", country, e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "지원하지 않는 국가입니다: " + country
            ));
            
        } catch (Exception e) {
            log.error("국가별 사업계획서 생성 중 오류 발생", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "사업계획서 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
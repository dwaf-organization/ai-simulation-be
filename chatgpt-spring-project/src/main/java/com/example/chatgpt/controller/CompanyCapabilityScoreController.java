package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.companycapabilityscore.respDto.CompanyCapabilityScoreRespDto;
import com.example.chatgpt.service.CompanyCapabilityScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CompanyCapabilityScoreController {
    
    private final CompanyCapabilityScoreService companyCapabilityScoreService;
    
    /**
     * 회사 역량 점수 조회 API
     * GET /api/v1/company-capability-score?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/company-capability-score")
    public ResponseEntity<RespDto<CompanyCapabilityScoreRespDto>> getCompanyCapabilityScore(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("회사 역량 점수 조회 API 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            CompanyCapabilityScoreRespDto result = companyCapabilityScoreService.getCompanyCapabilityScore(eventCode, teamCode);
            
            return ResponseEntity.ok(RespDto.success("회사 역량 점수 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("회사 역량 점수 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("회사 역량 점수 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("회사 역량 점수 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
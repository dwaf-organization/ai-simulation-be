package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.service.AdminTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminTriggerController {
    
    private final AdminTriggerService adminTriggerService;
    
    /**
     * 관리자 트리거 API - 요약보기 일괄처리
     * 1. event.summary_view_process 업데이트 (현재 스테이지 + 1)
     * 2. 해당 행사/스테이지의 모든 팀 매출 생성
     * 3. 팀별 순위 생성
     */
    @PostMapping("/api/admin/trigger/summary-view/{eventCode}/{stage}")
    public ResponseEntity<RespDto<String>> triggerSummaryView(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("stage") Integer stage) {
        
        try {
            log.info("관리자 트리거 요청 - eventCode: {}, stage: {}", eventCode, stage);
            
            String result = adminTriggerService.triggerSummaryViewProcess(eventCode, stage);
            
            return ResponseEntity.ok(RespDto.success("요약보기 일괄처리 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("트리거 처리 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("트리거 처리 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("처리 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * Stage1 전용 트리거 API - summary_view_process만 업데이트 (매출/지출 처리 없음)
     * POST /api/admin/trigger/summary-view/stage1/{eventCode}
     */
    @PostMapping("/api/admin/trigger/summary-view/stage1/{eventCode}")
    public ResponseEntity<RespDto<String>> triggerStage1SummaryView(
            @PathVariable("eventCode") Integer eventCode) {
        
        try {
            log.info("Stage1 트리거 요청 - eventCode: {}", eventCode);
            
            String result = adminTriggerService.triggerStage1SummaryView(eventCode);
            
            return ResponseEntity.ok(RespDto.success("Stage1 요약보기 처리 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("Stage1 트리거 처리 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Stage1 트리거 처리 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("Stage1 처리 중 오류가 발생했습니다."));
        }
    }
}
package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.service.StageSummaryViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StageSummaryViewController {

    private final StageSummaryViewService stageSummaryViewService;

    /**
     * 스테이지 요약 텍스트 조회
     */
    @GetMapping("/api/stage/summary/view/{eventCode}/{teamCode}/{stage}")
    public ResponseEntity<RespDto<String>> getStageSummaryText(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode, 
            @PathVariable("stage") Integer stage) {
        
        try {
            log.info("스테이지 요약 조회 요청 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stage);
            
            String summaryText = stageSummaryViewService.getSummaryText(eventCode, teamCode, stage);
            
            return ResponseEntity.ok(RespDto.success("요약 조회 성공", summaryText));
            
        } catch (RuntimeException e) {
            log.warn("요약 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("요약 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("요약 조회 중 오류가 발생했습니다."));
        }
    }
}
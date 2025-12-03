package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.stageranking.respDto.StageRankingRespDto;
import com.example.chatgpt.service.StageRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StageRankingController {
    
    private final StageRankingService stageRankingService;
    
    /**
     * 스테이지별 팀별순위 조회 API
     * GET /api/v1/stage-ranking/{eventCode}/{stageStep}
     */
    @GetMapping("/api/v1/stage-ranking/{eventCode}/{stageStep}")
    public ResponseEntity<RespDto<List<StageRankingRespDto>>> getStageRanking(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("stageStep") Integer stageStep) {
        
        try {
            log.info("스테이지별 팀별순위 조회 요청 - eventCode: {}, stageStep: {}", eventCode, stageStep);
            
            List<StageRankingRespDto> rankings = stageRankingService.getStageRanking(eventCode, stageStep);
            
            return ResponseEntity.ok(RespDto.success("스테이지별 팀별순위 조회 성공", rankings));
            
        } catch (RuntimeException e) {
            log.warn("스테이지별 팀별순위 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("스테이지별 팀별순위 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("스테이지별 팀별순위 조회 중 오류가 발생했습니다."));
        }
    }
}
package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.stage.reqDto.StageProgressReqDto;
import com.example.chatgpt.dto.stage.respDto.CurrentStageInfoRespDto;
import com.example.chatgpt.service.StageProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StageProgressController {
    
    private final StageProgressService stageProgressService;
    
    /**
     * 현재 스테이지/스텝 정보 조회 API
     * GET /api/v1/stage/current?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/stage/current")
    public ResponseEntity<RespDto<CurrentStageInfoRespDto>> getCurrentStageInfo(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("현재 스테이지 정보 조회 API 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            CurrentStageInfoRespDto result = stageProgressService.getCurrentStageInfo(eventCode, teamCode);
            
            return ResponseEntity.ok(RespDto.success("현재 스테이지 정보 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("현재 스테이지 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("현재 스테이지 정보 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("현재 스테이지 정보 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 다음 스텝으로 진행 API
     * POST /api/v1/stage/progress-step
     */
    @PostMapping("/api/v1/stage/progress-step")
    public ResponseEntity<RespDto<CurrentStageInfoRespDto>> progressToNextStep(
            @RequestBody StageProgressReqDto request) {
        
        try {
            log.info("다음 스텝 진행 API 요청 - eventCode: {}, teamCode: {}", 
                     request.getEventCode(), request.getTeamCode());
            
            CurrentStageInfoRespDto result = stageProgressService.progressToNextStep(
                request.getEventCode(), request.getTeamCode());
            
            return ResponseEntity.ok(RespDto.success("다음 스텝 진행 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("다음 스텝 진행 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("다음 스텝 진행 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("다음 스텝 진행 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 다음 스테이지로 진행 API
     * POST /api/v1/stage/progress-stage
     */
    @PostMapping("/api/v1/stage/progress-stage")
    public ResponseEntity<RespDto<CurrentStageInfoRespDto>> progressToNextStage(
            @RequestBody StageProgressReqDto request) {
        
        try {
            log.info("다음 스테이지 진행 API 요청 - eventCode: {}, teamCode: {}", 
                     request.getEventCode(), request.getTeamCode());
            
            CurrentStageInfoRespDto result = stageProgressService.progressToNextStage(
                request.getEventCode(), request.getTeamCode());
            
            return ResponseEntity.ok(RespDto.success("다음 스테이지 진행 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("다음 스테이지 진행 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("다음 스테이지 진행 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("다음 스테이지 진행 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
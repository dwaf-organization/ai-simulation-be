package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.llmquestion.respDto.LlmQuestionDto;
import com.example.chatgpt.dto.llmquestion.respDto.LlmQuestionListRespDto;
import com.example.chatgpt.service.LlmQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LlmQuestionController {
    
    private final LlmQuestionService llmQuestionService;
    
    /**
     * 스테이지별 질문 목록 조회 API
     * GET /api/v1/analyze/stage/list?eventCode=1&teamCode=5&stageStep=1
     */
    @GetMapping("/api/v1/analyze/stage/list")
    public ResponseEntity<RespDto<List<LlmQuestionDto>>> getStageQuestionList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode,
            @RequestParam("stageStep") Integer stageStep) {
        
        try {
            log.info("스테이지 질문 목록 조회 API 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            LlmQuestionListRespDto result = llmQuestionService.getStageQuestionList(eventCode, teamCode, stageStep);
            
            if (result.getQuestions().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("생성된 질문이 없습니다.", result.getQuestions()));
            } else {
                return ResponseEntity.ok(RespDto.success("질문 조회 성공", result.getQuestions()));
            }
            
        } catch (RuntimeException e) {
            log.warn("스테이지 질문 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("스테이지 질문 목록 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("질문 목록 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 팀의 모든 질문 조회 API (전체 스테이지)
     * GET /api/v1/analyze/team/questions?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/analyze/team/questions")
    public ResponseEntity<RespDto<List<LlmQuestionDto>>> getAllQuestionsByTeam(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("팀 전체 질문 조회 API 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            LlmQuestionListRespDto result = llmQuestionService.getAllQuestionsByTeam(eventCode, teamCode);
            
            if (result.getQuestions().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("생성된 질문이 없습니다.", result.getQuestions()));
            } else {
                return ResponseEntity.ok(RespDto.success("팀 전체 질문 조회 성공", result.getQuestions()));
            }
            
        } catch (RuntimeException e) {
            log.warn("팀 전체 질문 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("팀 전체 질문 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("팀 전체 질문 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
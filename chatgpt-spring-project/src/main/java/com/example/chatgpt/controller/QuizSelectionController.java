package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.quiz.reqDto.QuizSelectionLogReqDto;
import com.example.chatgpt.dto.quiz.respDto.QuizSelectionListDto;
import com.example.chatgpt.dto.quiz.respDto.QuizSelectionListRespDto;
import com.example.chatgpt.service.QuizSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QuizSelectionController {
    
    private final QuizSelectionService quizSelectionService;
    
    /**
     * 퀴즈 선택 로그 저장 API
     * POST /api/v1/quiz-selection/log
     */
    @PostMapping("/api/v1/quiz-selection/log")
    public ResponseEntity<RespDto<Integer>> logQuizSelection(
            @Valid @RequestBody QuizSelectionLogReqDto request) {
        
        try {
            log.info("퀴즈 선택 로그 API 요청 - quizCode: {}, teamCode: {}, stageStep: {}", 
                     request.getQuizCode(), request.getTeamCode(), request.getStageStep());
            
            Integer quizSelectionCode = quizSelectionService.logQuizSelection(request);
            
            return ResponseEntity.ok(RespDto.success("퀴즈 선택 로그 저장 성공", quizSelectionCode));
            
        } catch (RuntimeException e) {
            log.warn("퀴즈 선택 로그 저장 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("퀴즈 선택 로그 저장 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("퀴즈 선택 로그 저장 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 퀴즈 선택 목록 조회 API
     * GET /api/v1/quiz-selection/list?teamCode=5&stageStep=1
     */
    @GetMapping("/api/v1/quiz-selection/list")
    public ResponseEntity<RespDto<List<QuizSelectionListDto>>> getQuizSelectionList(
            @RequestParam("teamCode") Integer teamCode,
            @RequestParam("stageStep") Integer stageStep) {
        
        try {
            log.info("퀴즈 선택 목록 조회 API 요청 - teamCode: {}, stageStep: {}", teamCode, stageStep);
            
            QuizSelectionListRespDto result = quizSelectionService.getQuizSelectionList(teamCode, stageStep);
            
            if (result.getQuizSelections().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("퀴즈 선택 이력이 없습니다.", result.getQuizSelections()));
            } else {
                return ResponseEntity.ok(RespDto.success("퀴즈 선택 목록 조회 성공", result.getQuizSelections()));
            }
            
        } catch (RuntimeException e) {
            log.warn("퀴즈 선택 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("퀴즈 선택 목록 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("퀴즈 선택 목록 조회 중 오류가 발생했습니다."));
        }
    }
}
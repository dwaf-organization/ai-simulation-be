package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.surprisequestion.reqDto.SurpriseQuestionSelectionReqDto;
import com.example.chatgpt.dto.surprisequestion.reqDto.SurpriseQuestionSubjectiveAnswerReqDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionListDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionListRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSelectionRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveListDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveListRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveAnswerRespDto;
import com.example.chatgpt.service.SurpriseQuestionService;
import com.example.chatgpt.service.SurpriseQuestionAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SurpriseQuestionController {
    
    private final SurpriseQuestionService surpriseQuestionService;
    private final SurpriseQuestionAnswerService surpriseQuestionAnswerService;
    
    /**
     * 팀의 수익모델에 따른 랜덤 객관식 돌발질문 조회 API
     * GET /api/v1/surprise-question/{teamCode}
     */
    @GetMapping("/api/v1/surprise-question/{teamCode}")
    public ResponseEntity<RespDto<SurpriseQuestionRespDto>> getSurpriseQuestion(
            @PathVariable("teamCode") Integer teamCode) {
        
        try {
            log.info("객관식 돌발질문 API 요청 - teamCode: {}", teamCode);
            
            // 랜덤 객관식 돌발질문 조회
            SurpriseQuestionRespDto result = surpriseQuestionService.getRandomSurpriseQuestion(teamCode);
            
            return ResponseEntity.ok(RespDto.success("객관식 돌발질문 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("객관식 돌발질문 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("객관식 돌발질문 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("객관식 돌발질문 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 팀의 수익모델에 따른 랜덤 주관식 돌발질문 조회 API
     * GET /api/v1/surprise-question-subjective/{teamCode}
     */
    @GetMapping("/api/v1/surprise-question-subjective/{teamCode}")
    public ResponseEntity<RespDto<SurpriseQuestionSubjectiveRespDto>> getSubjectiveSurpriseQuestion(
            @PathVariable("teamCode") Integer teamCode) {
        
        try {
            log.info("주관식 돌발질문 API 요청 - teamCode: {}", teamCode);
            
            // 랜덤 주관식 돌발질문 조회
            SurpriseQuestionSubjectiveRespDto result = surpriseQuestionService.getRandomSubjectiveSurpriseQuestion(teamCode);
            
            return ResponseEntity.ok(RespDto.success("주관식 돌발질문 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("주관식 돌발질문 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("주관식 돌발질문 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("주관식 돌발질문 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 객관식 돌발질문 답변 제출 및 AI 피드백 생성 API
     * POST /api/v1/surprise-question/submit-answer
     */
    @PostMapping("/api/v1/surprise-question/submit-answer")
    public ResponseEntity<RespDto<SurpriseQuestionSelectionRespDto>> submitSurpriseQuestionAnswer(
            @RequestBody SurpriseQuestionSelectionReqDto request) {
        
        try {
            log.info("객관식 돌발질문 답변 제출 API 요청 - sqCode: {}, teamCode: {}, answer: {}", 
                     request.getSqCode(), request.getTeamCode(), request.getSqAnswer());
            
            // 답변 제출 및 AI 피드백 생성
            SurpriseQuestionSelectionRespDto result = surpriseQuestionAnswerService.submitSurpriseQuestionAnswer(request);
            
            return ResponseEntity.ok(RespDto.success("객관식 돌발질문 답변 제출 및 AI 피드백 생성 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("객관식 돌발질문 답변 제출 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("객관식 돌발질문 답변 제출 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("객관식 돌발질문 답변 제출 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 주관식 돌발질문 답변 제출 및 AI 피드백 생성 API
     * POST /api/v1/surprise-question-subjective/submit-answer
     */
    @PostMapping("/api/v1/surprise-question-subjective/submit-answer")
    public ResponseEntity<RespDto<SurpriseQuestionSubjectiveAnswerRespDto>> submitSubjectiveSurpriseQuestionAnswer(
            @RequestBody SurpriseQuestionSubjectiveAnswerReqDto request) {
        
        try {
            log.info("주관식 돌발질문 답변 제출 API 요청 - sqSubjCode: {}, teamCode: {}, answerLength: {}자", 
                     request.getSqSubjCode(), request.getTeamCode(), 
                     request.getAnswerText() != null ? request.getAnswerText().length() : 0);
            
            // 주관식 답변 제출 및 AI 피드백 생성
            SurpriseQuestionSubjectiveAnswerRespDto result = surpriseQuestionAnswerService.submitSubjectiveSurpriseQuestionAnswer(request);
            
            return ResponseEntity.ok(RespDto.success("주관식 돌발질문 답변 제출 및 AI 피드백 생성 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("주관식 돌발질문 답변 제출 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("주관식 돌발질문 답변 제출 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("주관식 돌발질문 답변 제출 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 객관식 돌발질문 목록 조회 API (답변 + 질문 정보)
     * GET /api/v1/surprise-question/list?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/surprise-question/list")
    public ResponseEntity<RespDto<List<SurpriseQuestionListDto>>> getSurpriseQuestionList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("객관식 돌발질문 목록 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            SurpriseQuestionListRespDto result = surpriseQuestionService.getSurpriseQuestionList(eventCode, teamCode);
            
            if (result.getSurpriseQuestions().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("객관식 돌발질문 답변이 없습니다.", result.getSurpriseQuestions()));
            } else {
                return ResponseEntity.ok(RespDto.success("객관식 돌발질문 목록 조회 성공", result.getSurpriseQuestions()));
            }
            
        } catch (RuntimeException e) {
            log.warn("객관식 돌발질문 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("객관식 돌발질문 목록 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("객관식 돌발질문 목록 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 주관식 돌발질문 목록 조회 API (답변 + 질문 정보)
     * GET /api/v1/surprise-question-subjective/list?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/surprise-question-subjective/list")
    public ResponseEntity<RespDto<List<SurpriseQuestionSubjectiveListDto>>> getSurpriseQuestionSubjectiveList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("주관식 돌발질문 목록 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            SurpriseQuestionSubjectiveListRespDto result = surpriseQuestionService.getSurpriseQuestionSubjectiveList(eventCode, teamCode);
            
            if (result.getSurpriseQuestions().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("주관식 돌발질문 답변이 없습니다.", result.getSurpriseQuestions()));
            } else {
                return ResponseEntity.ok(RespDto.success("주관식 돌발질문 목록 조회 성공", result.getSurpriseQuestions()));
            }
            
        } catch (RuntimeException e) {
            log.warn("주관식 돌발질문 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("주관식 돌발질문 목록 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("주관식 돌발질문 목록 조회 중 오류가 발생했습니다."));
        }
    }
}
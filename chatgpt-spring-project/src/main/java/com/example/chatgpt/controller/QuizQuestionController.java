package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.quiz.respDto.QuizQuestionDto;
import com.example.chatgpt.service.QuizQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QuizQuestionController {
    
    private final QuizQuestionService quizQuestionService;
    
    /**
     * 랜덤 퀴즈 조회 API
     * GET /api/v1/quiz-question/random
     */
    @GetMapping("/api/v1/quiz-question/random")
    public ResponseEntity<RespDto<QuizQuestionDto>> getRandomQuiz() {
        
        try {
            log.info("랜덤 퀴즈 조회 API 요청");
            
            QuizQuestionDto result = quizQuestionService.getRandomQuiz();
            
            return ResponseEntity.ok(RespDto.success("랜덤 퀴즈 조회 성공", result));
            
        } catch (RuntimeException e) {
            log.warn("랜덤 퀴즈 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("랜덤 퀴즈 조회 중 예상치 못한 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("퀴즈 조회 중 오류가 발생했습니다."));
        }
    }
}
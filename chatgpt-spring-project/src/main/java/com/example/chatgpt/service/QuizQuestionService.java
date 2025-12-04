package com.example.chatgpt.service;

import com.example.chatgpt.dto.quiz.respDto.QuizQuestionDto;
import com.example.chatgpt.entity.QuizQuestion;
import com.example.chatgpt.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizQuestionService {
    
    private final QuizQuestionRepository quizQuestionRepository;
    
    /**
     * 랜덤 퀴즈 조회
     */
    public QuizQuestionDto getRandomQuiz() {
        try {
            log.info("랜덤 퀴즈 조회 요청");
            
            // 1. 전체 퀴즈 개수 확인
            long totalCount = quizQuestionRepository.countAllQuizzes();
            if (totalCount == 0) {
                log.warn("데이터베이스에 퀴즈가 없습니다");
                throw new RuntimeException("퀴즈 데이터가 없습니다.");
            }
            
            // 2. 랜덤 퀴즈 조회
            Optional<QuizQuestion> optionalQuiz = quizQuestionRepository.findRandomQuiz();
            
            if (optionalQuiz.isEmpty()) {
                log.error("랜덤 퀴즈 조회 실패 - 데이터가 있지만 조회되지 않음");
                throw new RuntimeException("퀴즈 조회에 실패했습니다.");
            }
            
            QuizQuestion quiz = optionalQuiz.get();
            
            // 3. DTO 변환
            QuizQuestionDto result = QuizQuestionDto.from(quiz);
            
            log.info("랜덤 퀴즈 조회 완료 - quizCode: {}, 질문: \"{}\"", 
                     quiz.getQuizCode(), 
                     quiz.getQuestionText().length() > 20 ? 
                         quiz.getQuestionText().substring(0, 20) + "..." : 
                         quiz.getQuestionText());
            
            return result;
            
        } catch (RuntimeException e) {
            log.error("랜덤 퀴즈 조회 중 비즈니스 오류: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("랜덤 퀴즈 조회 중 시스템 오류", e);
            throw new RuntimeException("퀴즈 조회 중 시스템 오류가 발생했습니다.");
        }
    }
}
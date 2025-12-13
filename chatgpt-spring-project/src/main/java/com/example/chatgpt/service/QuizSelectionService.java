package com.example.chatgpt.service;

import com.example.chatgpt.dto.quiz.reqDto.QuizSelectionLogReqDto;
import com.example.chatgpt.dto.quiz.respDto.QuizSelectionListDto;
import com.example.chatgpt.dto.quiz.respDto.QuizSelectionListRespDto;
import com.example.chatgpt.entity.QuizQuestion;
import com.example.chatgpt.entity.QuizSelection;
import com.example.chatgpt.repository.QuizQuestionRepository;
import com.example.chatgpt.repository.QuizSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizSelectionService {
    
    private final QuizSelectionRepository quizSelectionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    
    /**
     * 퀴즈 선택 로그 저장
     */
    @Transactional
    public Integer logQuizSelection(QuizSelectionLogReqDto request) {
        try {
            log.info("퀴즈 선택 로그 저장 - quizCode: {}, teamCode: {}, stageStep: {}", 
                     request.getQuizCode(), request.getTeamCode(), request.getStageStep());
            
            // 1. 퀴즈 존재 여부 확인
            Optional<QuizQuestion> quizOpt = quizQuestionRepository.findById(request.getQuizCode());
            if (quizOpt.isEmpty()) {
                throw new RuntimeException("존재하지 않는 퀴즈입니다.");
            }
            
            // 2. 중복 체크
            boolean exists = quizSelectionRepository.existsByQuizCodeAndTeamCode(
                request.getQuizCode(), request.getTeamCode());
            
            if (exists) {
                log.warn("이미 선택한 퀴즈입니다 - quizCode: {}, teamCode: {}", 
                         request.getQuizCode(), request.getTeamCode());
                throw new RuntimeException("이미 선택한 퀴즈입니다.");
            }
            
            // 3. 새 로그 저장
            QuizSelection newSelection = QuizSelection.builder()
                .quizCode(request.getQuizCode())
                .teamCode(request.getTeamCode())
                .stageStep(request.getStageStep())
                .build();
            
            QuizSelection savedSelection = quizSelectionRepository.save(newSelection);
            
            log.info("퀴즈 선택 로그 저장 완료 - quizSelectionCode: {}", savedSelection.getQuizSelectionCode());
            return savedSelection.getQuizSelectionCode();
            
        } catch (RuntimeException e) {
            log.error("퀴즈 선택 로그 저장 실패: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("퀴즈 선택 로그 저장 중 시스템 오류", e);
            throw new RuntimeException("퀴즈 선택 로그 저장 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 팀의 스테이지별 퀴즈 선택 목록 조회
     */
    public QuizSelectionListRespDto getQuizSelectionList(Integer teamCode, Integer stageStep) {
        try {
            log.info("퀴즈 선택 목록 조회 - teamCode: {}, stageStep: {}", teamCode, stageStep);
            
            // 1. 팀의 스테이지별 퀴즈 선택 목록 조회
            List<QuizSelection> selections = quizSelectionRepository
                .findByTeamCodeAndStageStepOrderByCreatedAt(teamCode, stageStep);
            
            if (selections.isEmpty()) {
                log.info("퀴즈 선택 이력이 없음 - teamCode: {}, stageStep: {}", teamCode, stageStep);
                return QuizSelectionListRespDto.empty();
            }
            
            List<QuizSelectionListDto> resultList = new ArrayList<>();
            
            // 2. 각 선택에 대해 퀴즈 정보와 조인
            for (QuizSelection selection : selections) {
                Optional<QuizQuestion> questionOpt = quizQuestionRepository.findById(selection.getQuizCode());
                
                if (questionOpt.isPresent()) {
                    QuizQuestion question = questionOpt.get();
                    QuizSelectionListDto dto = QuizSelectionListDto.from(selection, question);
                    resultList.add(dto);
                } else {
                    log.warn("퀴즈 정보를 찾을 수 없음 - quizCode: {}", selection.getQuizCode());
                }
            }
            
            log.info("퀴즈 선택 목록 조회 완료 - {}개 항목", resultList.size());
            return QuizSelectionListRespDto.from(resultList);
            
        } catch (Exception e) {
            log.error("퀴즈 선택 목록 조회 실패", e);
            throw new RuntimeException("퀴즈 선택 목록 조회 실패: " + e.getMessage());
        }
    }
}
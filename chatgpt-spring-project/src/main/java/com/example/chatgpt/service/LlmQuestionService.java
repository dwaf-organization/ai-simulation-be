package com.example.chatgpt.service;

import com.example.chatgpt.dto.llmquestion.respDto.LlmQuestionListRespDto;
import com.example.chatgpt.entity.LlmQuestion;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.repository.LlmQuestionRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LlmQuestionService {
    
    private final LlmQuestionRepository llmQuestionRepository;
    private final TeamMstRepository teamMstRepository;
    
    /**
     * 특정 팀의 특정 스테이지 질문 목록 조회
     */
    public LlmQuestionListRespDto getStageQuestionList(Integer eventCode, Integer teamCode, Integer stageStep) {
        try {
            log.info("스테이지 질문 목록 조회 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            // 1. 팀 존재 여부 확인
            Optional<TeamMst> teamOpt = teamMstRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            if (teamOpt.isEmpty()) {
                throw new RuntimeException("존재하지 않는 팀입니다.");
            }
            
            // 2. 해당 팀의 스테이지별 질문 조회
            List<LlmQuestion> questionList = llmQuestionRepository.findByTeamCodeAndStageStep(teamCode, stageStep);
            
            if (questionList.isEmpty()) {
                log.info("생성된 질문이 없음 - teamCode: {}, stageStep: {}", teamCode, stageStep);
                return LlmQuestionListRespDto.empty();
            }
            
            log.info("질문 목록 조회 완료 - teamCode: {}, stageStep: {}, 질문 수: {}개", 
                     teamCode, stageStep, questionList.size());
            
            return LlmQuestionListRespDto.from(questionList);
            
        } catch (Exception e) {
            log.error("스테이지 질문 목록 조회 실패", e);
            throw new RuntimeException("질문 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 특정 팀의 질문 존재 여부 확인
     */
    public boolean hasQuestions(Integer teamCode, Integer stageStep) {
        try {
            return llmQuestionRepository.existsByTeamCodeAndStageStep(teamCode, stageStep);
        } catch (Exception e) {
            log.error("질문 존재 여부 확인 실패", e);
            return false;
        }
    }
    
    /**
     * 특정 팀의 모든 질문 조회
     */
    public LlmQuestionListRespDto getAllQuestionsByTeam(Integer eventCode, Integer teamCode) {
        try {
            log.info("팀 전체 질문 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀 존재 여부 확인
            Optional<TeamMst> teamOpt = teamMstRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            if (teamOpt.isEmpty()) {
                throw new RuntimeException("존재하지 않는 팀입니다.");
            }
            
            // 2. 해당 팀의 모든 질문 조회
            List<LlmQuestion> questionList = llmQuestionRepository.findByTeamCode(teamCode);
            
            if (questionList.isEmpty()) {
                log.info("생성된 질문이 없음 - teamCode: {}", teamCode);
                return LlmQuestionListRespDto.empty();
            }
            
            log.info("팀 전체 질문 조회 완료 - teamCode: {}, 총 질문 수: {}개", teamCode, questionList.size());
            
            return LlmQuestionListRespDto.from(questionList);
            
        } catch (Exception e) {
            log.error("팀 전체 질문 조회 실패", e);
            throw new RuntimeException("팀 전체 질문 조회 실패: " + e.getMessage());
        }
    }
}
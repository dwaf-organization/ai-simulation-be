package com.example.chatgpt.service;

import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionListDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionListRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveListDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveListRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveRespDto;
import com.example.chatgpt.entity.RevenueModel;
import com.example.chatgpt.entity.SurpriseQuestion;
import com.example.chatgpt.entity.SurpriseQuestionAnswer;
import com.example.chatgpt.entity.SurpriseQuestionSelection;
import com.example.chatgpt.entity.SurpriseQuestionSubjective;
import com.example.chatgpt.repository.RevenueModelRepository;
import com.example.chatgpt.repository.SurpriseQuestionAnswerRepository;
import com.example.chatgpt.repository.SurpriseQuestionRepository;
import com.example.chatgpt.repository.SurpriseQuestionSelectionRepository;
import com.example.chatgpt.repository.SurpriseQuestionSubjectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SurpriseQuestionService {
    
    private final SurpriseQuestionRepository surpriseQuestionRepository;
    private final SurpriseQuestionSubjectiveRepository surpriseQuestionSubjectiveRepository;
    private final SurpriseQuestionSelectionRepository surpriseQuestionSelectionRepository;
    private final SurpriseQuestionAnswerRepository surpriseQuestionAnswerRepository;
    private final RevenueModelRepository revenueModelRepository;
    private final Random random = new Random();
    
    /**
     * 객관식 돌발질문 목록 조회 (답변 + 질문 정보)
     */
    public SurpriseQuestionListRespDto getSurpriseQuestionList(Integer eventCode, Integer teamCode) {
        try {
            log.info("객관식 돌발질문 목록 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀의 객관식 돌발질문 답변 목록 조회
            List<SurpriseQuestionSelection> selections = surpriseQuestionSelectionRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (selections.isEmpty()) {
                log.info("객관식 돌발질문 답변이 없음 - eventCode: {}, teamCode: {}", eventCode, teamCode);
                return SurpriseQuestionListRespDto.empty();
            }
            
            List<SurpriseQuestionListDto> resultList = new ArrayList<>();
            
            // 2. 각 답변에 대해 질문 정보와 조인
            for (SurpriseQuestionSelection selection : selections) {
                Optional<SurpriseQuestion> questionOpt = surpriseQuestionRepository.findById(selection.getSqCode());
                
                if (questionOpt.isPresent()) {
                    SurpriseQuestion question = questionOpt.get();
                    SurpriseQuestionListDto dto = SurpriseQuestionListDto.from(selection, question);
                    resultList.add(dto);
                } else {
                    log.warn("객관식 돌발질문을 찾을 수 없음 - sqCode: {}", selection.getSqCode());
                }
            }
            
            log.info("객관식 돌발질문 목록 조회 완료 - {}개 항목", resultList.size());
            return SurpriseQuestionListRespDto.from(resultList);
            
        } catch (Exception e) {
            log.error("객관식 돌발질문 목록 조회 실패", e);
            throw new RuntimeException("객관식 돌발질문 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 주관식 돌발질문 목록 조회 (답변 + 질문 정보)
     */
    public SurpriseQuestionSubjectiveListRespDto getSurpriseQuestionSubjectiveList(Integer eventCode, Integer teamCode) {
        try {
            log.info("주관식 돌발질문 목록 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀의 주관식 돌발질문 답변 목록 조회
            List<SurpriseQuestionAnswer> answers = surpriseQuestionAnswerRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (answers.isEmpty()) {
                log.info("주관식 돌발질문 답변이 없음 - eventCode: {}, teamCode: {}", eventCode, teamCode);
                return SurpriseQuestionSubjectiveListRespDto.empty();
            }
            
            List<SurpriseQuestionSubjectiveListDto> resultList = new ArrayList<>();
            
            // 2. 각 답변에 대해 질문 정보와 조인
            for (SurpriseQuestionAnswer answer : answers) {
                Optional<SurpriseQuestionSubjective> questionOpt = surpriseQuestionSubjectiveRepository.findById(answer.getSqSubjCode());
                
                if (questionOpt.isPresent()) {
                    SurpriseQuestionSubjective question = questionOpt.get();
                    SurpriseQuestionSubjectiveListDto dto = SurpriseQuestionSubjectiveListDto.from(answer, question);
                    resultList.add(dto);
                } else {
                    log.warn("주관식 돌발질문을 찾을 수 없음 - sqSubjCode: {}", answer.getSqSubjCode());
                }
            }
            
            log.info("주관식 돌발질문 목록 조회 완료 - {}개 항목", resultList.size());
            return SurpriseQuestionSubjectiveListRespDto.from(resultList);
            
        } catch (Exception e) {
            log.error("주관식 돌발질문 목록 조회 실패", e);
            throw new RuntimeException("주관식 돌발질문 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 팀의 수익모델에 따른 랜덤 객관식 돌발질문 조회
     * @param teamCode 팀 코드
     * @return 랜덤 객관식 돌발질문
     */
    public SurpriseQuestionRespDto getRandomSurpriseQuestion(Integer teamCode) {
        try {
            log.info("객관식 돌발질문 조회 요청 - teamCode: {}", teamCode);
            
            // 1. 팀의 수익모델 조회
            RevenueModel revenueModel = revenueModelRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new RuntimeException("팀의 수익모델 데이터가 없습니다."));
            
            Integer revenueCategory = revenueModel.getRevenueCategory();
            log.info("팀 {}의 수익모델 카테고리: {}", teamCode, revenueCategory);
            
            // 2. 해당 카테고리의 객관식 돌발질문 목록 조회
            List<SurpriseQuestion> questions = surpriseQuestionRepository.findByCategoryCode(revenueCategory);
            
            if (questions.isEmpty()) {
                throw new RuntimeException("해당 카테고리(" + revenueCategory + ")의 객관식 돌발질문이 없습니다.");
            }
            
            // 3. 랜덤으로 질문 선택
            int randomIndex = random.nextInt(questions.size());
            SurpriseQuestion selectedQuestion = questions.get(randomIndex);
            
            log.info("객관식 돌발질문 선택 완료 - sqCode: {}, 카테고리: {}, 제목: {}", 
                     selectedQuestion.getSqCode(), revenueCategory, selectedQuestion.getCardTitle());
            
            // 4. DTO 변환 (정답과 힌트 제외)
            return SurpriseQuestionRespDto.from(selectedQuestion);
            
        } catch (Exception e) {
            log.error("객관식 돌발질문 조회 실패", e);
            throw new RuntimeException("객관식 돌발질문 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 팀의 수익모델에 따른 랜덤 주관식 돌발질문 조회
     * @param teamCode 팀 코드
     * @return 랜덤 주관식 돌발질문
     */
    public SurpriseQuestionSubjectiveRespDto getRandomSubjectiveSurpriseQuestion(Integer teamCode) {
        try {
            log.info("주관식 돌발질문 조회 요청 - teamCode: {}", teamCode);
            
            // 1. 팀의 수익모델 조회
            RevenueModel revenueModel = revenueModelRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new RuntimeException("팀의 수익모델 데이터가 없습니다."));
            
            Integer revenueCategory = revenueModel.getRevenueCategory();
            log.info("팀 {}의 수익모델 카테고리: {}", teamCode, revenueCategory);
            
            // 2. 해당 카테고리의 주관식 돌발질문 목록 조회
            List<SurpriseQuestionSubjective> questions = surpriseQuestionSubjectiveRepository.findByCategoryCode(revenueCategory);
            
            if (questions.isEmpty()) {
                throw new RuntimeException("해당 카테고리(" + revenueCategory + ")의 주관식 돌발질문이 없습니다.");
            }
            
            // 3. 랜덤으로 질문 선택
            int randomIndex = random.nextInt(questions.size());
            SurpriseQuestionSubjective selectedQuestion = questions.get(randomIndex);
            
            log.info("주관식 돌발질문 선택 완료 - sqSubjCode: {}, 카테고리: {}, 제목: {}", 
                     selectedQuestion.getSqSubjCode(), revenueCategory, selectedQuestion.getCardTitle());
            
            // 4. DTO 변환
            return SurpriseQuestionSubjectiveRespDto.from(selectedQuestion);
            
        } catch (Exception e) {
            log.error("주관식 돌발질문 조회 실패", e);
            throw new RuntimeException("주관식 돌발질문 조회 실패: " + e.getMessage());
        }
    }
}
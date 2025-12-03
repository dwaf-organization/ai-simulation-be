package com.example.chatgpt.service;

import com.example.chatgpt.dto.surprisequestion.reqDto.SurpriseQuestionSelectionReqDto;
import com.example.chatgpt.dto.surprisequestion.reqDto.SurpriseQuestionSubjectiveAnswerReqDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSelectionRespDto;
import com.example.chatgpt.dto.surprisequestion.respDto.SurpriseQuestionSubjectiveAnswerRespDto;
import com.example.chatgpt.entity.GroupSummary;
import com.example.chatgpt.entity.SurpriseQuestion;
import com.example.chatgpt.entity.SurpriseQuestionAnswer;
import com.example.chatgpt.entity.SurpriseQuestionSelection;
import com.example.chatgpt.entity.SurpriseQuestionSubjective;
import com.example.chatgpt.repository.GroupSummaryRepository;
import com.example.chatgpt.repository.SurpriseQuestionAnswerRepository;
import com.example.chatgpt.repository.SurpriseQuestionRepository;
import com.example.chatgpt.repository.SurpriseQuestionSelectionRepository;
import com.example.chatgpt.repository.SurpriseQuestionSubjectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SurpriseQuestionAnswerService {
    
    private final SurpriseQuestionSelectionRepository surpriseQuestionSelectionRepository;
    private final SurpriseQuestionAnswerRepository surpriseQuestionAnswerRepository;
    private final SurpriseQuestionRepository surpriseQuestionRepository;
    private final SurpriseQuestionSubjectiveRepository surpriseQuestionSubjectiveRepository;
    private final GroupSummaryRepository groupSummaryRepository;
    private final OpenAiService openAiService; // ChatGPT API
    
    /**
     * 객관식 돌발질문 답변 선택 및 AI 피드백 생성
     */
    @Transactional
    public SurpriseQuestionSelectionRespDto submitSurpriseQuestionAnswer(SurpriseQuestionSelectionReqDto request) {
        try {
            log.info("객관식 돌발질문 답변 제출 - sqCode: {}, teamCode: {}, answer: {}", 
                     request.getSqCode(), request.getTeamCode(), request.getSqAnswer());
            
            // 1. 돌발질문 정보 조회
            SurpriseQuestion question = surpriseQuestionRepository.findById(request.getSqCode())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 돌발질문입니다."));
            
            // 2. 팀의 스테이지 요약 조회 (6개가 아니어도 가능)
            List<GroupSummary> stageSummaries = groupSummaryRepository
                .findByEventCodeAndTeamCodeOrderByStageStep(request.getEventCode(), request.getTeamCode());
            
            log.info("팀 {}의 스테이지 요약 개수: {}개", request.getTeamCode(), stageSummaries.size());
            
            // 3. AI 피드백 생성
            String aiFeedback = generateObjectiveFeedback(question, stageSummaries, request.getSqAnswer());
            
            // 4. 답변 저장 (중복 체크 후 덮어쓰기 또는 신규 생성)
            SurpriseQuestionSelection selection = saveOrUpdateObjectiveSelection(request, aiFeedback);
            
            log.info("객관식 돌발질문 답변 저장 완료 - selectionCode: {}", selection.getSqSelectionCode());
            return SurpriseQuestionSelectionRespDto.from(selection);
            
        } catch (Exception e) {
            log.error("객관식 돌발질문 답변 처리 실패", e);
            throw new RuntimeException("객관식 돌발질문 답변 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 주관식 돌발질문 답변 제출 및 AI 피드백 생성
     */
    @Transactional
    public SurpriseQuestionSubjectiveAnswerRespDto submitSubjectiveSurpriseQuestionAnswer(SurpriseQuestionSubjectiveAnswerReqDto request) {
        try {
            log.info("주관식 돌발질문 답변 제출 - sqSubjCode: {}, teamCode: {}, answer: {}", 
                     request.getSqSubjCode(), request.getTeamCode(), request.getAnswerText());
            
            // 1. 주관식 돌발질문 정보 조회
            SurpriseQuestionSubjective question = surpriseQuestionSubjectiveRepository.findById(request.getSqSubjCode())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 주관식 돌발질문입니다."));
            
            // 2. 팀의 스테이지 요약 조회
            List<GroupSummary> stageSummaries = groupSummaryRepository
                .findByEventCodeAndTeamCodeOrderByStageStep(request.getEventCode(), request.getTeamCode());
            
            log.info("팀 {}의 스테이지 요약 개수: {}개", request.getTeamCode(), stageSummaries.size());
            
            // 3. AI 피드백 생성
            String aiFeedback = generateSubjectiveFeedback(question, stageSummaries, request.getAnswerText());
            
            // 4. 답변 저장 (중복 체크 후 덮어쓰기 또는 신규 생성)
            SurpriseQuestionAnswer answer = saveOrUpdateSubjectiveAnswer(request, aiFeedback);
            
            log.info("주관식 돌발질문 답변 저장 완료 - answerCode: {}", answer.getSqAnswerCode());
            return SurpriseQuestionSubjectiveAnswerRespDto.from(answer);
            
        } catch (Exception e) {
            log.error("주관식 돌발질문 답변 처리 실패", e);
            throw new RuntimeException("주관식 돌발질문 답변 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 객관식 AI 피드백 생성
     */
    private String generateObjectiveFeedback(SurpriseQuestion question, List<GroupSummary> stageSummaries, String selectedAnswer) {
        try {
            // 프롬프트 구성
            String prompt = createObjectiveFeedbackPrompt(question, stageSummaries, selectedAnswer);
            
            log.info("객관식 AI 피드백 생성 요청 - 질문: {}", question.getCardTitle());
            
            // ChatGPT API 호출 (OpenAiService 사용)
            String feedback = openAiService.chat(prompt);
            
            if (feedback == null || feedback.trim().isEmpty()) {
                throw new RuntimeException("AI 피드백 생성 실패: 빈 응답");
            }
            
            log.info("객관식 AI 피드백 생성 완료 - 길이: {}자", feedback.length());
            return feedback;
            
        } catch (Exception e) {
            log.error("객관식 AI 피드백 생성 중 오류 발생", e);
            throw new RuntimeException("객관식 AI 피드백 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 주관식 AI 피드백 생성
     */
    private String generateSubjectiveFeedback(SurpriseQuestionSubjective question, List<GroupSummary> stageSummaries, String answerText) {
        try {
            // 프롬프트 구성
            String prompt = createSubjectiveFeedbackPrompt(question, stageSummaries, answerText);
            
            log.info("주관식 AI 피드백 생성 요청 - 질문: {}", question.getCardTitle());
            
            // ChatGPT API 호출 (OpenAiService 사용)
            String feedback = openAiService.chat(prompt);
            
            if (feedback == null || feedback.trim().isEmpty()) {
                throw new RuntimeException("AI 피드백 생성 실패: 빈 응답");
            }
            
            log.info("주관식 AI 피드백 생성 완료 - 길이: {}자", feedback.length());
            return feedback;
            
        } catch (Exception e) {
            log.error("주관식 AI 피드백 생성 중 오류 발생", e);
            throw new RuntimeException("주관식 AI 피드백 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 객관식 피드백 생성용 프롬프트 구성
     */
    private String createObjectiveFeedbackPrompt(SurpriseQuestion question, List<GroupSummary> stageSummaries, String selectedAnswer) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 경험이 풍부한 창업 멘토입니다.\n\n");
        
        // 1. 팀의 사업 현황 (존재하는 스테이지만 추가)
        prompt.append("## 팀의 사업 현황\n");
        if (stageSummaries.isEmpty()) {
            prompt.append("**현재 사업 정보 없음** - 초기 단계 팀으로 추정됩니다.\n\n");
        } else {
            for (GroupSummary summary : stageSummaries) {
                if (summary.getSummaryText() != null && !summary.getSummaryText().trim().isEmpty()) {
                    prompt.append(String.format("**%d단계**: %s\n\n", summary.getStageStep(), summary.getSummaryText()));
                }
            }
        }
        
        // 2. 돌발질문 상황
        prompt.append("## 돌발 상황\n");
        prompt.append("**제목**: ").append(question.getCardTitle()).append("\n");
        if (question.getSituationDescription() != null && !question.getSituationDescription().trim().isEmpty()) {
            prompt.append("**상황 설명**: ").append(question.getSituationDescription()).append("\n");
        }
        prompt.append("**질문**: ").append(question.getQuestionText()).append("\n\n");
        
        // 3. 선택지들
        prompt.append("## 선택지\n");
        prompt.append("1. ").append(question.getOption1()).append("\n");
        prompt.append("2. ").append(question.getOption2()).append("\n");
        prompt.append("3. ").append(question.getOption3()).append("\n\n");
        
        // 4. 선택한 답변
        prompt.append("## 팀이 선택한 답변\n");
        prompt.append("**선택**: ").append(selectedAnswer).append("\n\n");
        
        // 5. 분석 요청
        prompt.append("## 멘토링 요청\n");
        prompt.append("위의 사업 현황과 돌발 상황을 종합적으로 분석하여 다음 내용을 제공해주세요:\n\n");
        prompt.append("### 1. 선택에 대한 분석\n");
        prompt.append("- 팀이 선택한 답변의 장점과 단점\n");
        prompt.append("- 이 선택으로 인해 발생할 수 있는 예상 상황들\n");
        
        // 사업 현황이 있는 경우와 없는 경우 구분
        if (!stageSummaries.isEmpty()) {
            prompt.append("- 팀의 현재 사업 단계와 상황을 고려한 평가\n\n");
        } else {
            prompt.append("- 초기 단계 스타트업 관점에서의 평가\n\n");
        }
        
        prompt.append("### 2. AI 멘토의 추천\n");
        prompt.append("- 이 상황에서 가장 합리적이라고 판단되는 선택지 (1, 2, 3 중)\n");
        prompt.append("- 해당 선택을 추천하는 구체적인 이유\n");
        
        if (!stageSummaries.isEmpty()) {
            prompt.append("- 팀의 현재 상황에서 왜 이 선택이 최적인지 설명\n\n");
        } else {
            prompt.append("- 초기 스타트업 단계에서 왜 이 선택이 최적인지 설명\n\n");
        }
        
        prompt.append("### 3. 추가 조언\n");
        prompt.append("- 이런 상황에서 고려해야 할 다른 요소들\n");
        prompt.append("- 향후 유사한 상황을 대비하는 방법\n\n");
        prompt.append("**형식**: 마크다운으로 작성하되, 친근하고 실용적인 멘토링 톤으로 작성해주세요.\n");
        prompt.append("**길이**: 충분히 상세하게 (최소 500자 이상)");
        
        return prompt.toString();
    }
    
    /**
     * 주관식 피드백 생성용 프롬프트 구성
     */
    private String createSubjectiveFeedbackPrompt(SurpriseQuestionSubjective question, List<GroupSummary> stageSummaries, String answerText) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 경험이 풍부한 창업 멘토입니다.\n\n");
        
        // 1. 팀의 사업 현황 (존재하는 스테이지만 추가)
        prompt.append("## 팀의 사업 현황\n");
        if (stageSummaries.isEmpty()) {
            prompt.append("**현재 사업 정보 없음** - 초기 단계 팀으로 추정됩니다.\n\n");
        } else {
            for (GroupSummary summary : stageSummaries) {
                if (summary.getSummaryText() != null && !summary.getSummaryText().trim().isEmpty()) {
                    prompt.append(String.format("**%d단계**: %s\n\n", summary.getStageStep(), summary.getSummaryText()));
                }
            }
        }
        
        // 2. 돌발질문 상황
        prompt.append("## 돌발 상황\n");
        prompt.append("**제목**: ").append(question.getCardTitle()).append("\n");
        if (question.getSituationDescription() != null && !question.getSituationDescription().trim().isEmpty()) {
            prompt.append("**상황 설명**: ").append(question.getSituationDescription()).append("\n");
        }
        prompt.append("**질문**: ").append(question.getQuestionText()).append("\n\n");
        
        // 3. 팀의 주관식 답변
        prompt.append("## 팀의 답변\n");
        prompt.append("**답변 내용**: ").append(answerText).append("\n\n");
        
        // 4. 분석 요청
        prompt.append("## 멘토링 요청\n");
        prompt.append("위의 사업 현황과 돌발 상황, 그리고 팀의 답변을 종합적으로 분석하여 다음 내용을 제공해주세요:\n\n");
        prompt.append("### 1. 답변에 대한 분석\n");
        prompt.append("- 팀이 제시한 답변의 창의성과 실현 가능성\n");
        prompt.append("- 답변에서 나타난 팀의 사고 방식과 접근법 평가\n");
        prompt.append("- 이 답변으로 인해 발생할 수 있는 예상 결과들\n");
        
        // 사업 현황이 있는 경우와 없는 경우 구분
        if (!stageSummaries.isEmpty()) {
            prompt.append("- 팀의 현재 사업 단계와 역량을 고려한 답변 적합성 평가\n\n");
        } else {
            prompt.append("- 초기 단계 스타트업 관점에서의 답변 적합성 평가\n\n");
        }
        
        prompt.append("### 2. AI 멘토의 개선 제안\n");
        prompt.append("- 답변에서 놓친 중요한 관점이나 고려사항\n");
        prompt.append("- 더 효과적인 접근 방법이나 대안 제시\n");
        prompt.append("- 답변을 보완하거나 발전시킬 수 있는 구체적인 방안\n\n");
        
        prompt.append("### 3. 실행 가이드\n");
        prompt.append("- 팀의 답변을 실제로 실행하기 위한 단계별 가이드\n");
        prompt.append("- 실행 과정에서 주의해야 할 리스크와 대응 방안\n");
        prompt.append("- 성공을 위해 필요한 추가 자원이나 역량\n\n");
        
        prompt.append("### 4. 추가 조언\n");
        prompt.append("- 이런 유형의 문제 해결 역량을 기르는 방법\n");
        prompt.append("- 향후 유사한 상황에서 더 나은 답변을 위한 팁\n\n");
        
        prompt.append("**형식**: 마크다운으로 작성하되, 건설적이고 격려하는 멘토링 톤으로 작성해주세요.\n");
        prompt.append("**길이**: 충분히 상세하게 (최소 600자 이상)");
        
        return prompt.toString();
    }
    
    /**
     * 객관식 답변 저장 또는 업데이트
     */
    private SurpriseQuestionSelection saveOrUpdateObjectiveSelection(SurpriseQuestionSelectionReqDto request, String aiFeedback) {
        // 기존 답변 확인
        Optional<SurpriseQuestionSelection> existing = surpriseQuestionSelectionRepository
            .findBySqCodeAndTeamCode(request.getSqCode(), request.getTeamCode());
        
        if (existing.isPresent()) {
            // 기존 답변 업데이트
            SurpriseQuestionSelection selection = existing.get();
            selection.setSqAnswer(request.getSqAnswer());
            selection.setAiFeedback(aiFeedback);
            
            log.info("기존 객관식 돌발질문 답변 업데이트 - selectionCode: {}", selection.getSqSelectionCode());
            return surpriseQuestionSelectionRepository.save(selection);
        } else {
            // 신규 답변 생성
            SurpriseQuestionSelection newSelection = SurpriseQuestionSelection.builder()
                .sqCode(request.getSqCode())
                .eventCode(request.getEventCode())
                .teamCode(request.getTeamCode())
                .sqAnswer(request.getSqAnswer())
                .aiFeedback(aiFeedback)
                .build();
            
            log.info("신규 객관식 돌발질문 답변 생성");
            return surpriseQuestionSelectionRepository.save(newSelection);
        }
    }
    
    /**
     * 주관식 답변 저장 또는 업데이트
     */
    private SurpriseQuestionAnswer saveOrUpdateSubjectiveAnswer(SurpriseQuestionSubjectiveAnswerReqDto request, String aiFeedback) {
        // 기존 답변 확인
        Optional<SurpriseQuestionAnswer> existing = surpriseQuestionAnswerRepository
            .findBySqSubjCodeAndTeamCode(request.getSqSubjCode(), request.getTeamCode());
        
        if (existing.isPresent()) {
            // 기존 답변 업데이트
            SurpriseQuestionAnswer answer = existing.get();
            answer.setAnswerText(request.getAnswerText());
            answer.setAiFeedback(aiFeedback);
            
            log.info("기존 주관식 돌발질문 답변 업데이트 - answerCode: {}", answer.getSqAnswerCode());
            return surpriseQuestionAnswerRepository.save(answer);
        } else {
            // 신규 답변 생성
            SurpriseQuestionAnswer newAnswer = SurpriseQuestionAnswer.builder()
                .sqSubjCode(request.getSqSubjCode())
                .eventCode(request.getEventCode())
                .teamCode(request.getTeamCode())
                .answerText(request.getAnswerText())
                .aiFeedback(aiFeedback)
                .build();
            
            log.info("신규 주관식 돌발질문 답변 생성");
            return surpriseQuestionAnswerRepository.save(newAnswer);
        }
    }
}
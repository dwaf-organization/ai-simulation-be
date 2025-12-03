package com.example.chatgpt.service;

import com.example.chatgpt.entity.GroupSummary;
import com.example.chatgpt.repository.GroupSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StageSummaryViewService {

    private final GroupSummaryRepository groupSummaryRepository;

    /**
     * 스테이지 요약 텍스트 조회
     */
    public String getSummaryText(Integer eventCode, Integer teamCode, Integer stage) {
        log.info("요약 텍스트 조회 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stage);
        
        // GroupSummary에서 summary_text 조회
        Optional<GroupSummary> summaryOpt = groupSummaryRepository.findByEventCodeAndTeamCodeAndStageStep(
            eventCode, teamCode, stage);
        
        if (summaryOpt.isEmpty()) {
            throw new RuntimeException("해당 스테이지의 요약을 찾을 수 없습니다.");
        }
        
        GroupSummary summary = summaryOpt.get();
        String summaryText = summary.getSummaryText();
        
        if (summaryText == null || summaryText.trim().isEmpty()) {
            throw new RuntimeException("요약 텍스트가 생성되지 않았습니다.");
        }
        
        log.info("요약 텍스트 조회 성공 - 길이: {}자", summaryText.length());
        
        return summaryText;
    }
}
package com.example.chatgpt.service;

import com.example.chatgpt.dto.stage.respDto.CurrentStageInfoRespDto;
import com.example.chatgpt.dto.stage.respDto.StageInfoDto;
import com.example.chatgpt.entity.StageMst;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.repository.StageMstRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StageProgressService {
    
    private final TeamMstRepository teamMstRepository;
    private final StageMstRepository stageMstRepository;
    
    /**
     * 현재 스테이지/스텝 정보 조회
     */
    public CurrentStageInfoRespDto getCurrentStageInfo(Integer eventCode, Integer teamCode) {
        try {
            log.info("현재 스테이지 정보 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀 정보 조회
            TeamMst team = teamMstRepository.findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("팀 정보를 찾을 수 없습니다."));
            
            // 2. 현재 스테이지의 모든 스텝 조회
            List<StageMst> stageMstList = stageMstRepository.findByStageIdOrderByStepIdAsc(team.getCurrentStageId());
            
            if (stageMstList.isEmpty()) {
                throw new RuntimeException("스테이지 정보를 찾을 수 없습니다. 스테이지 ID: " + team.getCurrentStageId());
            }
            
            // 3. DTO 변환
            List<StageInfoDto> stageInfoList = stageMstList.stream()
                .map(StageInfoDto::from)
                .collect(Collectors.toList());
            
            log.info("현재 스테이지 정보 조회 완료 - 현재 스테이지: {}, 현재 스텝: {}, 총 스텝 수: {}", 
                     team.getCurrentStageId(), team.getCurrentStepId(), stageInfoList.size());
            
            return CurrentStageInfoRespDto.from(team, stageInfoList);
            
        } catch (Exception e) {
            log.error("현재 스테이지 정보 조회 실패", e);
            throw new RuntimeException("현재 스테이지 정보 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 다음 스텝으로 진행 (currentStepId +1)
     */
    @Transactional
    public CurrentStageInfoRespDto progressToNextStep(Integer eventCode, Integer teamCode) {
        try {
            log.info("다음 스텝 진행 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀 존재 확인
            TeamMst team = teamMstRepository.findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("팀 정보를 찾을 수 없습니다."));
            
            // 2. 스텝 ID 업데이트
            int updatedCount = teamMstRepository.updateCurrentStepIdPlusOne(eventCode, teamCode);
            
            if (updatedCount == 0) {
                throw new RuntimeException("스텝 진행 업데이트에 실패했습니다.");
            }
            
            log.info("스텝 진행 완료 - 이전 스텝: {}, 새로운 스텝: {}", team.getCurrentStepId(), team.getCurrentStepId() + 1);
            
            // 3. 업데이트된 정보 조회 및 반환
            return getCurrentStageInfo(eventCode, teamCode);
            
        } catch (Exception e) {
            log.error("다음 스텝 진행 실패", e);
            throw new RuntimeException("다음 스텝 진행 실패: " + e.getMessage());
        }
    }
    
    /**
     * 다음 스테이지로 진행 (currentStageId +1, currentStepId = 1)
     */
    @Transactional
    public CurrentStageInfoRespDto progressToNextStage(Integer eventCode, Integer teamCode) {
        try {
            log.info("다음 스테이지 진행 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 팀 존재 확인
            TeamMst team = teamMstRepository.findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("팀 정보를 찾을 수 없습니다."));
            
            // 2. 스테이지 ID +1, 스텝 ID = 1로 업데이트
            int updatedCount = teamMstRepository.updateCurrentStageIdPlusOneAndStepIdToOne(eventCode, teamCode);
            
            if (updatedCount == 0) {
                throw new RuntimeException("스테이지 진행 업데이트에 실패했습니다.");
            }
            
            log.info("스테이지 진행 완료 - 이전 스테이지: {}, 새로운 스테이지: {}", team.getCurrentStageId(), team.getCurrentStageId() + 1);
            
            // 3. 업데이트된 정보 조회 및 반환
            return getCurrentStageInfo(eventCode, teamCode);
            
        } catch (Exception e) {
            log.error("다음 스테이지 진행 실패", e);
            throw new RuntimeException("다음 스테이지 진행 실패: " + e.getMessage());
        }
    }
}
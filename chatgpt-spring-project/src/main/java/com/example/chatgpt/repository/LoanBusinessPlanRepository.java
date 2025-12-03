package com.example.chatgpt.repository;

import com.example.chatgpt.entity.LoanBusinessPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanBusinessPlanRepository extends JpaRepository<LoanBusinessPlan, Integer> {
    
    /**
     * 특정 이벤트-팀-스테이지의 대출 사업계획서 조회 (중복 체크용)
     */
    Optional<LoanBusinessPlan> findByEventCodeAndTeamCodeAndStageStep(
        Integer eventCode, Integer teamCode, Integer stageStep);
    
    /**
     * 특정 팀의 모든 대출 사업계획서 조회
     */
    List<LoanBusinessPlan> findByEventCodeAndTeamCodeOrderByStageStep(
        Integer eventCode, Integer teamCode);
    
    /**
     * 중복 존재 여부 확인
     */
    boolean existsByEventCodeAndTeamCodeAndStageStep(
        Integer eventCode, Integer teamCode, Integer stageStep);
}
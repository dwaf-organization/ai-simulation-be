package com.example.chatgpt.repository;

import com.example.chatgpt.entity.LlmQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmQuestionRepository extends JpaRepository<LlmQuestion, Integer> {
    
    /**
     * 특정 팀의 특정 스테이지 질문 조회
     */
    @Query("SELECT lq FROM LlmQuestion lq WHERE lq.teamCode = :teamCode AND lq.stageStep = :stageStep ORDER BY lq.questionCode ASC")
    List<LlmQuestion> findByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 행사의 특정 스테이지 모든 질문 조회
     */
    @Query("SELECT lq FROM LlmQuestion lq WHERE lq.eventCode = :eventCode AND lq.stageStep = :stageStep ORDER BY lq.teamCode, lq.questionCode ASC")
    List<LlmQuestion> findByEventCodeAndStageStep(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 질문 존재 여부 확인
     */
    @Query("SELECT COUNT(lq) > 0 FROM LlmQuestion lq WHERE lq.teamCode = :teamCode AND lq.stageStep = :stageStep")
    boolean existsByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 모든 질문 조회
     */
    @Query("SELECT lq FROM LlmQuestion lq WHERE lq.teamCode = :teamCode ORDER BY lq.stageStep, lq.questionCode ASC")
    List<LlmQuestion> findByTeamCode(@Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 특정 스테이지 질문 삭제 (재생성용)
     */
    void deleteByTeamCodeAndStageStep(Integer teamCode, Integer stageStep);
}
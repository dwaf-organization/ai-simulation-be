package com.example.chatgpt.repository;

import com.example.chatgpt.entity.StageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageSummaryRepository extends JpaRepository<StageSummary, Integer> {
    
    /**
     * 팀코드와 스테이지로 요약본 조회
     */
    Optional<StageSummary> findByTeamCodeAndStageStep(Integer teamCode, Integer stageStep);
    
    /**
     * 팀코드로 모든 스테이지 요약본 조회 (스테이지 순서대로)
     */
    @Query("SELECT ss FROM StageSummary ss WHERE ss.teamCode = :teamCode ORDER BY ss.stageStep ASC")
    List<StageSummary> findByTeamCodeOrderByStageStep(@Param("teamCode") Integer teamCode);
    
    /**
     * 이벤트코드로 모든 팀의 요약본 조회
     */
    @Query("SELECT ss FROM StageSummary ss WHERE ss.eventCode = :eventCode ORDER BY ss.teamCode, ss.stageStep ASC")
    List<StageSummary> findByEventCodeOrderByTeamAndStage(@Param("eventCode") Integer eventCode);
    
    /**
     * 특정 팀의 특정 스테이지 요약 존재 여부 확인
     */
    @Query("SELECT COUNT(ss) > 0 FROM StageSummary ss WHERE ss.teamCode = :teamCode AND ss.stageStep = :stageStep")
    boolean existsByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 팀코드로 가장 최근 스테이지 요약본 조회
     */
    @Query("SELECT ss FROM StageSummary ss WHERE ss.teamCode = :teamCode ORDER BY ss.stageStep DESC LIMIT 1")
    Optional<StageSummary> findLatestByTeamCode(@Param("teamCode") Integer teamCode);
    
    /**
     * 특정 스테이지의 모든 팀 요약본 조회
     */
    @Query("SELECT ss FROM StageSummary ss WHERE ss.stageStep = :stageStep ORDER BY ss.teamCode ASC")
    List<StageSummary> findByStageStepOrderByTeamCode(@Param("stageStep") Integer stageStep);
    
    /**
     * 이벤트 내 특정 스테이지의 모든 팀 요약본 조회
     */
    @Query("SELECT ss FROM StageSummary ss WHERE ss.eventCode = :eventCode AND ss.stageStep = :stageStep ORDER BY ss.teamCode ASC")
    List<StageSummary> findByEventCodeAndStageStep(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    /**
     * 특정 팀의 모든 스테이지 요약 조회
     */
    @Query("SELECT ss FROM StageSummary ss WHERE ss.teamCode = :teamCode ORDER BY ss.stageStep ASC")
    List<StageSummary> findByTeamCode(@Param("teamCode") Integer teamCode);
    

    /**
     * 특정 팀의 특정 스테이지 요약 삭제 (덮어쓰기용)
     */
    @Modifying
    @Query("DELETE FROM StageSummary ss WHERE ss.teamCode = :teamCode AND ss.eventCode = :eventCode AND ss.stageStep = :stageStep")
    void deleteByTeamCodeAndEventCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
}
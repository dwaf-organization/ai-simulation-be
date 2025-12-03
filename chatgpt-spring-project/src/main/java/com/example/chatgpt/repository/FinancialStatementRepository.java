package com.example.chatgpt.repository;

import com.example.chatgpt.entity.FinancialStatement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialStatementRepository extends JpaRepository<FinancialStatement, Integer> {
    
    /**
     * 팀코드와 스테이지로 재무제표 조회
     */
    Optional<FinancialStatement> findByTeamCodeAndStageStep(Integer teamCode, Integer stageStep);
    
    /**
     * 팀코드로 모든 스테이지 재무제표 조회 (스테이지 순서대로)
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.teamCode = :teamCode ORDER BY fs.stageStep ASC")
    List<FinancialStatement> findByTeamCodeOrderByStageStep(@Param("teamCode") Integer teamCode);
    
    /**
     * 팀코드로 가장 최근 스테이지 재무제표 조회
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.teamCode = :teamCode ORDER BY fs.stageStep DESC LIMIT 1")
    Optional<FinancialStatement> findLatestByTeamCode(@Param("teamCode") Integer teamCode);
    
    /**
     * 이벤트코드로 모든 팀의 매출액 조회 (revenue_model 테이블 조인 필요 시 사용)
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.teamCode IN " +
           "(SELECT rm.teamCode FROM RevenueModel rm WHERE rm.eventCode = :eventCode)")
    List<FinancialStatement> findByEventCode(@Param("eventCode") Integer eventCode);
    
    /**
     * 팀, 행사, 스테이지별 재무상태표 조회
     */
    Optional<FinancialStatement> findByEventCodeAndTeamCodeAndStageStep(
        Integer eventCode, Integer teamCode, Integer stageStep);

    /**
     * 행사별, 스테이지별 모든 팀 재무상태표 조회
     */
    List<FinancialStatement> findByEventCodeAndStageStep(Integer eventCode, Integer stageStep);

    /**
     * 팀, 행사, 스테이지별 재무상태표 삭제 (덮어쓰기용)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FinancialStatement f WHERE f.eventCode = :eventCode AND f.teamCode = :teamCode AND f.stageStep = :stageStep")
    void deleteByEventCodeAndTeamCodeAndStageStep(
        @Param("eventCode") Integer eventCode, 
        @Param("teamCode") Integer teamCode, 
        @Param("stageStep") Integer stageStep);

    /**
     * 행사별, 스테이지별 모든 재무상태표 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FinancialStatement f WHERE f.eventCode = :eventCode AND f.stageStep = :stageStep")
    void deleteByEventCodeAndStageStep(
        @Param("eventCode") Integer eventCode, 
        @Param("stageStep") Integer stageStep);
}
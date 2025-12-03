package com.example.chatgpt.repository;

import com.example.chatgpt.entity.TeamRevenueAllocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 팀별 매출 분배 Repository
 */
@Repository
public interface TeamRevenueAllocationRepository extends JpaRepository<TeamRevenueAllocation, Integer> {
    
    /**
     * 분배 ID별 모든 팀 할당 조회 (순위순) - String 타입으로 수정
     */
    @Query("SELECT tra FROM TeamRevenueAllocation tra WHERE tra.distributionId = :distributionId ORDER BY tra.stageRank")
    List<TeamRevenueAllocation> findByDistributionIdOrderByStageRank(@Param("distributionId") String distributionId);
    
    /**
     * 이벤트-스테이지별 모든 팀 할당 조회 (순위순)
     */
    @Query("SELECT tra FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode AND tra.stageStep = :stageStep ORDER BY tra.stageRank")
    List<TeamRevenueAllocation> findByEventCodeAndStageStepOrderByStageRank(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 이벤트-스테이지별 모든 팀 할당 조회 (매출액 내림차순)
     */
    @Query("SELECT tra FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode AND tra.stageStep = :stageStep ORDER BY tra.allocatedRevenue DESC")
    List<TeamRevenueAllocation> findByEventCodeAndStageStepOrderByRevenueDesc(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 모든 할당 조회 (스테이지순)
     */
    @Query("SELECT tra FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode AND tra.teamCode = :teamCode ORDER BY tra.stageStep")
    List<TeamRevenueAllocation> findByEventCodeAndTeamCodeOrderByStageStep(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 특정 스테이지 할당 조회
     */
    Optional<TeamRevenueAllocation> findByEventCodeAndTeamCodeAndStageStep(Integer eventCode, Integer teamCode, Integer stageStep);
    
    /**
     * 이벤트별 모든 할당 조회 (스테이지, 순위 순)
     */
    @Query("SELECT tra FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode ORDER BY tra.stageStep, tra.stageRank")
    List<TeamRevenueAllocation> findByEventCodeOrderByStageAndRank(@Param("eventCode") Integer eventCode);
    
    /**
     * 팀별 누적 매출 통계
     */
    @Query("SELECT tra.teamCode, SUM(tra.allocatedRevenue) as totalRevenue, AVG(tra.stageRank) as avgRank, COUNT(tra) as stageCount " +
           "FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode " +
           "GROUP BY tra.teamCode ORDER BY totalRevenue DESC")
    List<Object[]> findTeamRevenueStatistics(@Param("eventCode") Integer eventCode);
    
    /**
     * 이벤트-스테이지별 매출 통계
     */
    @Query("SELECT " +
           "COUNT(tra) as teamCount, " +
           "AVG(tra.allocatedRevenue) as avgRevenue, " +
           "MIN(tra.allocatedRevenue) as minRevenue, " +
           "MAX(tra.allocatedRevenue) as maxRevenue, " +
           "SUM(tra.allocatedRevenue) as totalRevenue " +
           "FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode AND tra.stageStep = :stageStep")
    Object[] findRevenueStatisticsByEventCodeAndStageStep(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 최고 매출 팀 조회 (이벤트-스테이지별)
     */
    @Query("SELECT tra FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode AND tra.stageStep = :stageStep " +
           "AND tra.allocatedRevenue = (SELECT MAX(tra2.allocatedRevenue) FROM TeamRevenueAllocation tra2 " +
           "WHERE tra2.eventCode = :eventCode AND tra2.stageStep = :stageStep)")
    Optional<TeamRevenueAllocation> findTopRevenueTeam(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 분배 ID 존재 여부 확인 - String 타입으로 수정
     */
    boolean existsByDistributionId(String distributionId);
    
    /**
     * 분배 ID별 참여 팀 수 카운트 - String 타입으로 수정
     */
    @Query("SELECT COUNT(tra) FROM TeamRevenueAllocation tra WHERE tra.distributionId = :distributionId")
    Long countByDistributionId(@Param("distributionId") String distributionId);
    
    /**
     * 이벤트의 최신 분배 ID 조회 - String 타입으로 수정
     */
    @Query("SELECT MAX(tra.distributionId) FROM TeamRevenueAllocation tra WHERE tra.eventCode = :eventCode")
    Optional<String> findLatestDistributionIdByEventCode(@Param("eventCode") Integer eventCode);

    /**
     * 행사별, 스테이지별 모든 팀 매출 분배 조회 (순위별 정렬)
     */
    List<TeamRevenueAllocation> findByEventCodeAndStageStepOrderByStageRankAsc(
        Integer eventCode, Integer stageStep);

    /**
     * 행사별, 스테이지별 모든 매출 분배 삭제 (덮어쓰기용)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TeamRevenueAllocation t WHERE t.eventCode = :eventCode AND t.stageStep = :stageStep")
    void deleteByEventCodeAndStageStep(
        @Param("eventCode") Integer eventCode, 
        @Param("stageStep") Integer stageStep);

    /**
     * 특정 distribution_id로 매출 분배 조회
     */
    List<TeamRevenueAllocation> findByDistributionId(String distributionId);

    /**
     * 팀별 전체 스테이지 매출 분배 조회
     */
    List<TeamRevenueAllocation> findByTeamCodeOrderByStageStepAsc(Integer teamCode);
    
}
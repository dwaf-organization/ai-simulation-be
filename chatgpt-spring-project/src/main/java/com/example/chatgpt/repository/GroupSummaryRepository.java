package com.example.chatgpt.repository;

import com.example.chatgpt.entity.GroupSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 그룹 요약 정보 Repository
 */
@Repository
public interface GroupSummaryRepository extends JpaRepository<GroupSummary, Integer> {
    
    /**
     * 이벤트-스테이지별 모든 그룹 조회 (팀 코드 순)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.stageStep = :stageStep ORDER BY gs.teamCode")
    List<GroupSummary> findByEventCodeAndStageStepOrderByTeamCode(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 특정 스테이지 그룹 요약 조회
     */
    Optional<GroupSummary> findByEventCodeAndTeamCodeAndStageStep(Integer eventCode, Integer teamCode, Integer stageStep);
    
    /**
     * 특정 팀의 모든 스테이지 그룹 요약 조회 (스테이지 순)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.teamCode = :teamCode ORDER BY gs.stageStep")
    List<GroupSummary> findByEventCodeAndTeamCodeOrderByStageStep(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 이벤트별 모든 그룹 요약 조회 (스테이지, 팀 순)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode ORDER BY gs.stageStep, gs.teamCode")
    List<GroupSummary> findByEventCodeOrderByStageAndTeam(@Param("eventCode") Integer eventCode);
    
    /**
     * 이벤트-스테이지별 등록된 팀 수 카운트
     */
    @Query("SELECT COUNT(gs) FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.stageStep = :stageStep")
    Long countByEventCodeAndStageStep(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 완성된 요약 정보만 조회 (ChatGPT 저장 준비된 것들)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.stageStep = :stageStep " +
           "AND gs.businessType IS NOT NULL AND gs.summaryText IS NOT NULL ORDER BY gs.teamCode")
    List<GroupSummary> findCompletedSummariesByEventCodeAndStageStep(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 최근 생성된 그룹 요약들 조회 (최신 순)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode ORDER BY gs.createdAt DESC")
    List<GroupSummary> findByEventCodeOrderByCreatedAtDesc(@Param("eventCode") Integer eventCode);
    
    /**
     * 특정 팀의 모든 Stage 요약 조회
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.teamCode = :teamCode ORDER BY gs.stageStep ASC")
    List<GroupSummary> findByEventCodeAndTeamCode(
        @Param("eventCode") Integer eventCode,
        @Param("teamCode") Integer teamCode
    );
    
    /**
     * 특정 행사의 모든 팀 요약 조회
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode ORDER BY gs.teamCode ASC, gs.stageStep ASC")
    List<GroupSummary> findByEventCode(@Param("eventCode") Integer eventCode);
    
    /**
     * 특정 팀의 요약 존재 여부 확인
     */
    @Query("SELECT COUNT(gs) > 0 FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.teamCode = :teamCode AND gs.stageStep = :stageStep")
    boolean existsByEventCodeAndTeamCodeAndStageStep(
        @Param("eventCode") Integer eventCode,
        @Param("teamCode") Integer teamCode,
        @Param("stageStep") Integer stageStep
    );
    
    /**
     * 특정 행사/스테이지의 모든 팀 요약 조회 (관리자 트리거용)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.eventCode = :eventCode AND gs.stageStep = :stageStep ORDER BY gs.teamCode ASC")
    List<GroupSummary> findByEventCodeAndStageStep(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);

    /**
     * 특정 팀의 특정 스테이지 요약 삭제 (덮어쓰기용)
     */
    @Modifying
    @Query("DELETE FROM GroupSummary gs WHERE gs.teamCode = :teamCode AND gs.eventCode = :eventCode AND gs.stageStep = :stageStep")
    void deleteByTeamCodeAndEventCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 모든 스테이지 요약 조회 (1-6단계)
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.teamCode = :teamCode ORDER BY gs.stageStep ASC")
    List<GroupSummary> findAllByTeamCodeOrderByStageStep(@Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 특정 스테이지 요약 조회
     */
    @Query("SELECT gs FROM GroupSummary gs WHERE gs.teamCode = :teamCode AND gs.stageStep = :stageStep")
    Optional<GroupSummary> findByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
}
package com.example.chatgpt.repository;

import com.example.chatgpt.entity.ChatGptMemoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ChatGPT 메모리 로그 Repository (디버깅용)
 */
@Repository
public interface ChatGptMemoryLogRepository extends JpaRepository<ChatGptMemoryLog, Integer> {
    
    /**
     * 메모리 키로 조회
     */
    Optional<ChatGptMemoryLog> findByMemoryKey(String memoryKey);
    
    /**
     * 이벤트-스테이지별 모든 메모리 로그 조회 (팀 순)
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.eventCode = :eventCode AND cml.stageStep = :stageStep ORDER BY cml.teamCode")
    List<ChatGptMemoryLog> findByEventCodeAndStageStepOrderByTeamCode(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 메모리 로그 조회 (스테이지 역순)
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.eventCode = :eventCode AND cml.teamCode = :teamCode ORDER BY cml.stageStep DESC")
    List<ChatGptMemoryLog> findByEventCodeAndTeamCodeOrderByStageStepDesc(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 상태별 메모리 로그 조회 (최신순)
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.storageStatus = :status ORDER BY cml.createdAt DESC")
    List<ChatGptMemoryLog> findByStorageStatusOrderByCreatedAtDesc(@Param("status") String status);
    
    /**
     * 만료된 메모리 로그 조회
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.expiresAt < :now AND cml.storageStatus = 'STORED'")
    List<ChatGptMemoryLog> findExpiredMemoryLogs(@Param("now") LocalDateTime now);
    
    /**
     * 정상 상태 메모리 로그 조회 (이벤트-스테이지별)
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.eventCode = :eventCode AND cml.stageStep = :stageStep " +
           "AND cml.storageStatus = 'STORED' AND (cml.expiresAt IS NULL OR cml.expiresAt > :now) ORDER BY cml.teamCode")
    List<ChatGptMemoryLog> findHealthyMemoryLogs(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep, @Param("now") LocalDateTime now);
    
    /**
     * 이벤트별 저장된 메모리 수 카운트
     */
    @Query("SELECT COUNT(cml) FROM ChatGptMemoryLog cml WHERE cml.eventCode = :eventCode AND cml.storageStatus = 'STORED'")
    Long countStoredMemoryByEventCode(@Param("eventCode") Integer eventCode);
    
    /**
     * 이벤트-스테이지별 메모리 상태 통계
     */
    @Query("SELECT cml.storageStatus, COUNT(cml) FROM ChatGptMemoryLog cml " +
           "WHERE cml.eventCode = :eventCode AND cml.stageStep = :stageStep GROUP BY cml.storageStatus")
    List<Object[]> findMemoryStatusStatistics(@Param("eventCode") Integer eventCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 실패한 메모리 저장 조회
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.storageStatus = 'FAILED' ORDER BY cml.createdAt DESC")
    List<ChatGptMemoryLog> findFailedMemoryLogs();
    
    /**
     * 특정 기간 내 생성된 로그 조회
     */
    @Query("SELECT cml FROM ChatGptMemoryLog cml WHERE cml.createdAt BETWEEN :startDate AND :endDate ORDER BY cml.createdAt DESC")
    List<ChatGptMemoryLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * 메모리 키 존재 여부 확인
     */
    boolean existsByMemoryKey(String memoryKey);
}
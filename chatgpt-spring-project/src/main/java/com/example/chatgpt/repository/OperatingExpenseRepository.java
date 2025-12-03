package com.example.chatgpt.repository;

import com.example.chatgpt.entity.OperatingExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatingExpenseRepository extends JpaRepository<OperatingExpense, Integer> {
    
    /**
     * 특정 팀의 특정 스테이지 지출 조회
     */
    @Query("SELECT oe FROM OperatingExpense oe WHERE oe.teamCode = :teamCode AND oe.stageStep = :stageStep ORDER BY oe.expenseCode ASC")
    List<OperatingExpense> findByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 특정 스테이지 지출 존재 여부 확인
     */
    @Query("SELECT COUNT(oe) > 0 FROM OperatingExpense oe WHERE oe.teamCode = :teamCode AND oe.stageStep = :stageStep")
    boolean existsByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 특정 스테이지 지출 삭제 (재생성용)
     */
    @Modifying
    @Query("DELETE FROM OperatingExpense oe WHERE oe.teamCode = :teamCode AND oe.stageStep = :stageStep")
    void deleteByTeamCodeAndStageStep(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 특정 팀의 모든 지출 조회
     */
    @Query("SELECT oe FROM OperatingExpense oe WHERE oe.teamCode = :teamCode ORDER BY oe.stageStep, oe.expenseCode ASC")
    List<OperatingExpense> findByTeamCode(@Param("teamCode") Integer teamCode);
}
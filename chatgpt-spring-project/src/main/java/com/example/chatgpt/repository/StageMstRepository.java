package com.example.chatgpt.repository;

import com.example.chatgpt.entity.StageMst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageMstRepository extends JpaRepository<StageMst, Integer> {
    
    /**
     * 특정 스테이지 ID의 모든 스텝 조회 (스텝 ID 순서대로)
     */
    @Query("SELECT sm FROM StageMst sm WHERE sm.stageId = :stageId ORDER BY sm.stepId ASC")
    List<StageMst> findByStageIdOrderByStepIdAsc(@Param("stageId") Integer stageId);
}
package com.example.chatgpt.repository;

import com.example.chatgpt.entity.TeamMst;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface TeamMstRepository extends JpaRepository<TeamMst, Integer> {
    
    /**
     * 특정 행사의 팀 목록 조회 (최신순)
     */
    Page<TeamMst> findByEventCodeOrderByCreatedAtDesc(Integer eventCode, Pageable pageable);
    
    /**
     * 팀ID 중복 체크
     */
    boolean existsByTeamId(String teamId);
    
    /**
     * 특정 행사코드에 팀이 존재하는지 확인
     */
    boolean existsByEventCode(Integer eventCode);
    
    /**
     * 특정 행사의 팀 수 조회
     */
    @Query("SELECT COUNT(t) FROM TeamMst t WHERE t.eventCode = :eventCode")
    Integer countByEventCode(@Param("eventCode") Integer eventCode);
    
    
    /**
     * 이벤트 코드와 팀 코드로 팀 조회
     */
    @Query("SELECT tm FROM TeamMst tm WHERE tm.eventCode = :eventCode AND tm.teamCode = :teamCode")
    Optional<TeamMst> findByEventCodeAndTeamCode(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 현재 스텝 ID를 +1 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE TeamMst tm SET tm.currentStepId = tm.currentStepId + 1 WHERE tm.eventCode = :eventCode AND tm.teamCode = :teamCode")
    int updateCurrentStepIdPlusOne(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 현재 스테이지 ID를 +1, 스텝 ID를 1로 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE TeamMst tm SET tm.currentStageId = tm.currentStageId + 1, tm.currentStepId = 1 WHERE tm.eventCode = :eventCode AND tm.teamCode = :teamCode")
    int updateCurrentStageIdPlusOneAndStepIdToOne(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
}
package com.example.chatgpt.repository;

import com.example.chatgpt.entity.RevenueModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RevenueModelRepository extends JpaRepository<RevenueModel, Integer> {
    
    /**
     * 특정 팀의 수익모델 조회
     */
    @Query("SELECT rm FROM RevenueModel rm WHERE rm.teamCode = :teamCode")
    Optional<RevenueModel> findByTeamCode(@Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 수익모델 존재 여부 확인
     */
    @Query("SELECT COUNT(rm) > 0 FROM RevenueModel rm WHERE rm.teamCode = :teamCode")
    boolean existsByTeamCode(@Param("teamCode") Integer teamCode);
    
    /**
     * 특정 행사의 모든 수익모델 조회
     */
    @Query("SELECT rm FROM RevenueModel rm WHERE rm.eventCode = :eventCode ORDER BY rm.teamCode ASC")
    List<RevenueModel> findByEventCode(@Param("eventCode") Integer eventCode);
    
    /**
     * 특정 행사 + 팀의 수익모델 조회
     */
    @Query("SELECT rm FROM RevenueModel rm WHERE rm.eventCode = :eventCode AND rm.teamCode = :teamCode")
    Optional<RevenueModel> findByEventCodeAndTeamCode(@Param("eventCode") Integer eventCode, @Param("teamCode") Integer teamCode);
}
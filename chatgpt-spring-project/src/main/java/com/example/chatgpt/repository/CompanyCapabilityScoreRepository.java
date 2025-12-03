package com.example.chatgpt.repository;

import com.example.chatgpt.entity.CompanyCapabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyCapabilityScoreRepository extends JpaRepository<CompanyCapabilityScore, Integer> {
    
    /**
     * 특정 이벤트-팀의 역량 점수 조회
     */
    Optional<CompanyCapabilityScore> findByEventCodeAndTeamCode(Integer eventCode, Integer teamCode);
    
    /**
     * 특정 이벤트의 모든 팀 역량 점수 조회 (순위용)
     */
    List<CompanyCapabilityScore> findByEventCodeOrderByTotalCapabilityLevelDesc(Integer eventCode);
    
    /**
     * 특정 팀의 역량 점수 존재 여부 확인
     */
    boolean existsByEventCodeAndTeamCode(Integer eventCode, Integer teamCode);
    
    /**
     * 특정 팀코드로 역량 점수 삭제 (팀 삭제시 사용)
     */
    void deleteByTeamCode(Integer teamCode);
    
    /**
     * 특정 이벤트의 팀별 총 역량 순위 (Top N)
     */
    @Query("SELECT c FROM CompanyCapabilityScore c " +
           "WHERE c.eventCode = :eventCode " +
           "ORDER BY c.totalCapabilityLevel DESC")
    List<CompanyCapabilityScore> findTopCapabilityTeams(@Param("eventCode") Integer eventCode);
    
}
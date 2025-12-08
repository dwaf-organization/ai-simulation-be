package com.example.chatgpt.repository;

import com.example.chatgpt.entity.TeamDtl;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TeamDtlRepository extends JpaRepository<TeamDtl, Integer> {
    
    /**
     * 특정 팀의 팀원 목록 조회
     */
    List<TeamDtl> findByTeamCode(Integer teamCode);
    
    /**
     * 특정 팀의 팀원 수 조회
     */
    @Query("SELECT COUNT(td) FROM TeamDtl td WHERE td.teamCode = :teamCode")
    Integer countByTeamCode(@Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 모든 팀원 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TeamDtl td WHERE td.teamCode = :teamCode")
    Integer deleteByTeamCode(@Param("teamCode") Integer teamCode);
}
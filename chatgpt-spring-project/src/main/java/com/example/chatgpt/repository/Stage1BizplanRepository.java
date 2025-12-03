package com.example.chatgpt.repository;

import com.example.chatgpt.entity.Stage1Bizplan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Stage1BizplanRepository extends JpaRepository<Stage1Bizplan, Integer> {
    
    /**
     * 특정 팀의 사업계획서 조회
     */
    @Query("SELECT s FROM Stage1Bizplan s WHERE s.eventCode = :eventCode AND s.teamCode = :teamCode")
    Optional<Stage1Bizplan> findByEventCodeAndTeamCode(
        @Param("eventCode") Integer eventCode, 
        @Param("teamCode") Integer teamCode
    );
    
    /**
     * 특정 팀의 사업계획서 존재 여부 확인
     */
    @Query("SELECT COUNT(s) > 0 FROM Stage1Bizplan s WHERE s.eventCode = :eventCode AND s.teamCode = :teamCode")
    boolean existsByEventCodeAndTeamCode(
        @Param("eventCode") Integer eventCode, 
        @Param("teamCode") Integer teamCode
    );
}
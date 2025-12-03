package com.example.chatgpt.repository;

import com.example.chatgpt.entity.Stage6BizplanSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Stage6BizplanSummaryRepository extends JpaRepository<Stage6BizplanSummary, Integer> {
    
    /**
     * 특정 이벤트-팀의 Stage6 사업계획서 조회 (중복 체크용)
     */
    Optional<Stage6BizplanSummary> findByEventCodeAndTeamCode(Integer eventCode, Integer teamCode);
    
    /**
     * 특정 이벤트의 모든 Stage6 사업계획서 조회
     */
    List<Stage6BizplanSummary> findByEventCodeOrderByCreatedAtDesc(Integer eventCode);
    
    /**
     * 중복 존재 여부 확인
     */
    boolean existsByEventCodeAndTeamCode(Integer eventCode, Integer teamCode);
}
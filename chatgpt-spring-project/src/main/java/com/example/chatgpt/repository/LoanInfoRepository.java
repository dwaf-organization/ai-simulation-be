package com.example.chatgpt.repository;

import com.example.chatgpt.entity.LoanInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanInfoRepository extends JpaRepository<LoanInfo, Integer> {
    
    /**
     * 특정 이벤트-팀-스테이지의 대출정보 조회 (중복 체크용)
     */
    Optional<LoanInfo> findByEventCodeAndTeamCodeAndStageStep(
        Integer eventCode, Integer teamCode, Integer stageStep);
    
    /**
     * 특정 팀의 모든 대출정보 조회
     */
    List<LoanInfo> findByEventCodeAndTeamCodeOrderByStageStep(
        Integer eventCode, Integer teamCode);
    
    /**
     * 특정 이벤트의 모든 대출정보 조회
     */
    List<LoanInfo> findByEventCodeOrderByCreatedAtDesc(Integer eventCode);
    
    /**
     * 중복 존재 여부 확인
     */
    boolean existsByEventCodeAndTeamCodeAndStageStep(
        Integer eventCode, Integer teamCode, Integer stageStep);
}
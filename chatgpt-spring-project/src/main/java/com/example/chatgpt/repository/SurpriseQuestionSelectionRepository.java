package com.example.chatgpt.repository;

import com.example.chatgpt.entity.SurpriseQuestionSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurpriseQuestionSelectionRepository extends JpaRepository<SurpriseQuestionSelection, Integer> {
    
    /**
     * 특정 돌발질문과 팀의 답변 조회
     */
    @Query("SELECT sqs FROM SurpriseQuestionSelection sqs WHERE sqs.sqCode = :sqCode AND sqs.teamCode = :teamCode")
    Optional<SurpriseQuestionSelection> findBySqCodeAndTeamCode(@Param("sqCode") Integer sqCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 최근 돌발질문 답변 조회
     */
    @Query("SELECT sqs FROM SurpriseQuestionSelection sqs WHERE sqs.teamCode = :teamCode ORDER BY sqs.createdAt DESC")
    Optional<SurpriseQuestionSelection> findLatestByTeamCode(@Param("teamCode") Integer teamCode);
}
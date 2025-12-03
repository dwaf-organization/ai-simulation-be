package com.example.chatgpt.repository;

import com.example.chatgpt.entity.SurpriseQuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurpriseQuestionAnswerRepository extends JpaRepository<SurpriseQuestionAnswer, Integer> {
    
    /**
     * 특정 주관식 돌발질문과 팀의 답변 조회
     */
    @Query("SELECT sqa FROM SurpriseQuestionAnswer sqa WHERE sqa.sqSubjCode = :sqSubjCode AND sqa.teamCode = :teamCode")
    Optional<SurpriseQuestionAnswer> findBySqSubjCodeAndTeamCode(@Param("sqSubjCode") Integer sqSubjCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 최근 주관식 돌발질문 답변 조회
     */
    @Query("SELECT sqa FROM SurpriseQuestionAnswer sqa WHERE sqa.teamCode = :teamCode ORDER BY sqa.createdAt DESC")
    Optional<SurpriseQuestionAnswer> findLatestByTeamCode(@Param("teamCode") Integer teamCode);
}
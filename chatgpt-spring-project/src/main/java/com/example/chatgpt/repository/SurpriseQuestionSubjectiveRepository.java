package com.example.chatgpt.repository;

import com.example.chatgpt.entity.SurpriseQuestionSubjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurpriseQuestionSubjectiveRepository extends JpaRepository<SurpriseQuestionSubjective, Integer> {
    
    /**
     * 카테고리별 주관식 돌발질문 목록 조회
     */
    List<SurpriseQuestionSubjective> findByCategoryCode(Integer categoryCode);
    
    /**
     * 카테고리별 주관식 돌발질문 개수 조회
     */
    @Query("SELECT COUNT(sqs) FROM SurpriseQuestionSubjective sqs WHERE sqs.categoryCode = :categoryCode")
    Long countByCategoryCode(@Param("categoryCode") Integer categoryCode);
}
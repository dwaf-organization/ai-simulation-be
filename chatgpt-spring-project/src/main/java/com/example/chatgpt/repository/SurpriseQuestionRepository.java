package com.example.chatgpt.repository;

import com.example.chatgpt.entity.SurpriseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurpriseQuestionRepository extends JpaRepository<SurpriseQuestion, Integer> {
    
    /**
     * 카테고리별 돌발질문 목록 조회
     */
    List<SurpriseQuestion> findByCategoryCode(Integer categoryCode);
    
    /**
     * 카테고리별 돌발질문 개수 조회
     */
    @Query("SELECT COUNT(sq) FROM SurpriseQuestion sq WHERE sq.categoryCode = :categoryCode")
    Long countByCategoryCode(@Param("categoryCode") Integer categoryCode);
}
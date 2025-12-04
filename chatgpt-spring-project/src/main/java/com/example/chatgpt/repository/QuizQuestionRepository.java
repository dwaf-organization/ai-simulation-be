package com.example.chatgpt.repository;

import com.example.chatgpt.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {
    
    /**
     * 전체 퀴즈 개수 조회
     */
    @Query("SELECT COUNT(q) FROM QuizQuestion q")
    long countAllQuizzes();
    
    /**
     * 랜덤 퀴즈 하나 조회 (Native Query 사용)
     * MySQL의 RAND() 함수 사용
     */
    @Query(value = "SELECT * FROM quiz_question ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<QuizQuestion> findRandomQuiz();
}
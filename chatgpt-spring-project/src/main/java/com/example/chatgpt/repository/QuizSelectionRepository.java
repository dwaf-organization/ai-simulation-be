package com.example.chatgpt.repository;

import com.example.chatgpt.entity.QuizSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSelectionRepository extends JpaRepository<QuizSelection, Integer> {
    
    /**
     * 특정 퀴즈와 팀의 선택 로그 조회 (중복 체크용)
     */
    @Query("SELECT qs FROM QuizSelection qs WHERE qs.quizCode = :quizCode AND qs.teamCode = :teamCode")
    Optional<QuizSelection> findByQuizCodeAndTeamCode(@Param("quizCode") Integer quizCode, @Param("teamCode") Integer teamCode);
    
    /**
     * 특정 팀의 특정 스테이지 퀴즈 선택 목록 조회
     */
    @Query("SELECT qs FROM QuizSelection qs WHERE qs.teamCode = :teamCode AND qs.stageStep = :stageStep ORDER BY qs.createdAt ASC")
    List<QuizSelection> findByTeamCodeAndStageStepOrderByCreatedAt(@Param("teamCode") Integer teamCode, @Param("stageStep") Integer stageStep);
    
    /**
     * 중복 존재 여부 확인
     */
    @Query("SELECT COUNT(qs) > 0 FROM QuizSelection qs WHERE qs.quizCode = :quizCode AND qs.teamCode = :teamCode")
    boolean existsByQuizCodeAndTeamCode(@Param("quizCode") Integer quizCode, @Param("teamCode") Integer teamCode);
}
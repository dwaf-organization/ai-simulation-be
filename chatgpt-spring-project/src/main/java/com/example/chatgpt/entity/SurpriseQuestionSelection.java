package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "surprise_question_selection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionSelection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sq_selection_code", nullable = false, updatable = false)
    private Integer sqSelectionCode;
    
    @Column(name = "sq_code", nullable = false)
    private Integer sqCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "sq_answer", length = 250)
    private String sqAnswer;
    
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;  // AI가 생성한 피드백
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
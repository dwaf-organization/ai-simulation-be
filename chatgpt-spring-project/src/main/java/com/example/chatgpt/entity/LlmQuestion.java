package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_code")
    private Integer questionCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "selection_reason", columnDefinition = "TEXT")
    private String selectionReason;
    
    @Column(name = "question_summary")
    private String questionSummary;
    
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @Column(name = "option1")
    private String option1;
    
    @Column(name = "option2")
    private String option2;
    
    @Column(name = "option3")
    private String option3;
    
    @Column(name = "option4")
    private String option4;
    
    @Column(name = "option5")
    private String option5;
    
    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
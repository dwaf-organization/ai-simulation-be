package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {
    
    @Id
    @Column(name = "quiz_code")
    private Integer quizCode;
    
    @Column(name = "question_text")
    @Lob
    private String questionText;
    
    @Column(name = "option1", length = 250, nullable = false)
    private String option1;
    
    @Column(name = "option2", length = 250, nullable = false)
    private String option2;
    
    @Column(name = "option3", length = 250, nullable = false)
    private String option3;
    
    @Column(name = "option4", length = 250, nullable = false)
    private String option4;
    
    @Column(name = "hint_text", length = 250, nullable = false)
    private String hintText;
    
    @Column(name = "answer", nullable = false)
    private Integer answer;
    
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
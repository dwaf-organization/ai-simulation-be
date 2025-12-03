package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "surprise_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sq_code", nullable = false, updatable = false)
    private Integer sqCode;
    
    @Column(name = "category_code", nullable = false)
    private Integer categoryCode;
    
    @Column(name = "card_title", nullable = false, length = 200)
    private String cardTitle;
    
    @Column(name = "situation_description", nullable = false, columnDefinition = "TEXT")
    private String situationDescription;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "option1", length = 500)
    private String option1;
    
    @Column(name = "option2", length = 500)
    private String option2;
    
    @Column(name = "option3", length = 500)
    private String option3;
    
    @Column(name = "hint_text", length = 500)
    private String hintText;
    
    @Column(name = "answer")
    private Integer answer;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
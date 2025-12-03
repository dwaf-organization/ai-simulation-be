package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "surprise_question_subjective")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurpriseQuestionSubjective {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sq_subj_code", nullable = false, updatable = false)
    private Integer sqSubjCode;
    
    @Column(name = "category_code")
    private Integer categoryCode;
    
    @Column(name = "card_title", length = 250)
    private String cardTitle;
    
    @Column(name = "situation_description", columnDefinition = "TEXT")
    private String situationDescription;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
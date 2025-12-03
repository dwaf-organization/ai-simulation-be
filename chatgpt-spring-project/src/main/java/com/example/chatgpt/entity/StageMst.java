package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "stage_mst")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageMst {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage_step_code", nullable = false, updatable = false)
    private Integer stageStepCode;
    
    @Column(name = "stage_id", nullable = false)
    private Integer stageId;
    
    @Column(name = "step_id", nullable = false)
    private Integer stepId;
    
    @Column(name = "step_name", nullable = false, length = 250)
    private String stepName;
    
    @Column(name = "type", length = 250)
    private String type;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
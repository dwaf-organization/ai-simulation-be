package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_mst")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMst {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_code")
    private Integer teamCode;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_id", nullable = false, length = 100)
    private String teamId;
    
    @Column(name = "team_name", nullable = false, length = 250)
    private String teamName;
    
    @Column(name = "team_leader_name", length = 50)
    private String teamLeaderName;
    
    @Column(name = "team_image_url", length = 500)
    private String teamImageUrl;
    
    @Column(name = "current_stage_id", nullable = false)
    @Builder.Default
    private Integer currentStageId = 1;
    
    @Column(name = "current_step_id", nullable = false)
    @Builder.Default
    private Integer currentStepId = 1;
    
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
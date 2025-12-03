package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_dtl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDtl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_dtl_code")
    private Integer teamDtlCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "team_member_name", nullable = false, length = 100)
    private String teamMemberName;
    
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
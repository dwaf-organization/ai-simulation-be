package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stage1_bizplan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage1Bizplan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage1_code")
    private Integer stage1Code;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "bizplan_file_path", length = 250)
    private String bizplanFilePath;
    
    @Column(name = "bizplan_content", columnDefinition = "TEXT")
    private String bizplanContent;
    
    @Column(name = "biz_item_summary", columnDefinition = "TEXT")
    private String bizItemSummary;
    
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
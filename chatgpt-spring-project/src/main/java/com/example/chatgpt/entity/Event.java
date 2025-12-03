package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_code")
    private Integer eventCode;
    
    @Column(name = "event_name", nullable = false, length = 250)
    private String eventName;
    
    @Column(name = "event_status", nullable = false)
    private Integer eventStatus; // 1=진행중, 2=종료
    
    @Column(name = "event_at", length = 50)
    private String eventAt; // YYYY-MM-DD 형식
    
    @Column(name = "stage_batch_process")
    @Builder.Default
    private Integer stageBatchProcess = 1;
    
    @Column(name = "summary_view_process") 
    @Builder.Default
    private Integer summaryViewProcess = 1;
    
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
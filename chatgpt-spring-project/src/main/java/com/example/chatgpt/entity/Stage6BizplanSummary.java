package com.example.chatgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stage6_bizplan_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage6BizplanSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage6_code")
    private Integer stage6Code;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "bizplan_file_path", length = 500)
    private String bizplanFilePath;
    
    @Column(name = "biz_item_summary")
    @Lob
    private String bizItemSummary;
    
    @Column(name = "global_bizplan_file_path", length = 500)
    private String globalBizplanFilePath;
    
    @Column(name = "global_biz_item_summary")
    @Lob
    private String globalBizItemSummary;
    
    @Column(name = "usa_summary")
    @Lob
    private String usaSummary;
    
    @Column(name = "china_summary")
    @Lob
    private String chinaSummary;
    
    @Column(name = "japan_summary")
    @Lob
    private String japanSummary;
    
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
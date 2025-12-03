package com.example.chatgpt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// Spring Boot 3.0+ (Jakarta EE)
import jakarta.persistence.*;

// Spring Boot 2.x 이하라면 아래 주석을 해제하고 위 import를 제거하세요
// import javax.persistence.*;

import java.time.LocalDateTime;

/**
 * 그룹별 핵심 정보 요약 Entity
 * ChatGPT 매출 분배를 위한 기초 데이터 저장
 */
@Entity
@Table(name = "group_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Integer summaryId;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;
    
    @Column(name = "business_type", length = 200)
    private String businessType; // 사업 유형
    
    @Column(name = "core_technology", length = 300)
    private String coreTechnology; // 핵심 기술/역량
    
    @Column(name = "revenue_model", length = 300)
    private String revenueModel; // 수익 모델
    
    @Column(name = "key_answers", columnDefinition = "TEXT")
    private String keyAnswers; // 주요 답변 요약
    
    @Column(name = "investment_scale", length = 200)
    private String investmentScale; // 투자 규모
    
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths; // 강점
    
    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses; // 약점
    
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText; // ChatGPT 저장용 압축 요약
    
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
    
    /**
     * ChatGPT 메모리 키 생성
     */
    public String generateMemoryKey() {
        return String.format("event%d_team%d_stage%d", eventCode, teamCode, stageStep);
    }
    
    /**
     * 요약 정보가 완성되었는지 확인
     */
    public boolean isComplete() {
        return businessType != null && coreTechnology != null && 
               revenueModel != null && summaryText != null;
    }
}
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
 * 팀별 매출 분배 결과 및 순위 관리 Entity
 */
@Entity
@Table(name = "team_revenue_allocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRevenueAllocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Integer allocationId;
    
    @Column(name = "distribution_id", nullable = false)
    private String distributionId; // 같은 시점 분배 그룹핑용
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;
    
    @Column(name = "allocated_revenue", nullable = false)
    private Long allocatedRevenue; // 분배된 월 매출액 (원 단위)
    
    @Column(name = "stage_rank")
    private Integer stageRank; // 해당 스테이지 순위 (매출액 기준)
    
    @Column(name = "allocation_reason", columnDefinition = "TEXT")
    private String allocationReason; // ChatGPT 분배 근거/이유
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * 매출액을 만원 단위로 변환
     */
    public Long getRevenueInTenThousand() {
        return allocatedRevenue != null ? allocatedRevenue / 10000 : 0L;
    }
    
    /**
     * 매출액을 포맷된 문자열로 변환
     */
    public String getFormattedRevenue() {
        if (allocatedRevenue == null) return "0원";
        
        if (allocatedRevenue >= 100000000) { // 1억 이상
            return String.format("%.1f억원", allocatedRevenue / 100000000.0);
        } else if (allocatedRevenue >= 10000) { // 1만 이상
            return String.format("%,d만원", allocatedRevenue / 10000);
        } else {
            return String.format("%,d원", allocatedRevenue);
        }
    }
    
    /**
     * 순위에 따른 메달 아이콘 반환
     */
    public String getRankIcon() {
        if (stageRank == null) return "";
        
        switch (stageRank) {
            case 1: return "🥇";
            case 2: return "🥈"; 
            case 3: return "🥉";
            default: return String.valueOf(stageRank);
        }
    }
    
    /**
     * 분배 근거 요약 (100자 이내)
     */
    public String getShortReason() {
        if (allocationReason == null || allocationReason.length() <= 100) {
            return allocationReason;
        }
        return allocationReason.substring(0, 97) + "...";
    }
}
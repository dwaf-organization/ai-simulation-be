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
 * ChatGPT 메모리 저장 이력 관리 Entity (디버깅용)
 */
@Entity
@Table(name = "chatgpt_memory_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatGptMemoryLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;
    
    @Column(name = "event_code", nullable = false)
    private Integer eventCode;
    
    @Column(name = "team_code", nullable = false)
    private Integer teamCode;
    
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;
    
    @Column(name = "memory_key", length = 100, nullable = false, unique = true)
    private String memoryKey; // ChatGPT 메모리 키
    
    @Column(name = "stored_content", columnDefinition = "TEXT", nullable = false)
    private String storedContent; // ChatGPT에 저장된 압축 내용
    
    @Column(name = "storage_status", length = 20)
    private String storageStatus = "STORED"; // STORED/RETRIEVED/EXPIRED/FAILED
    
    @Column(name = "chatgpt_response", columnDefinition = "TEXT")
    private String chatgptResponse; // ChatGPT 저장 확인 응답
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 메모리 예상 만료 시간
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 기본 만료시간: 7일 후
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(7);
        }
    }
    
    /**
     * 스토리지 상태 열거형
     */
    public enum StorageStatus {
        STORED("저장 완료"),
        RETRIEVED("조회됨"),
        EXPIRED("만료됨"),
        FAILED("저장 실패");
        
        private final String description;
        
        StorageStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 메모리가 만료되었는지 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 메모리 상태가 정상인지 확인
     */
    public boolean isHealthy() {
        return "STORED".equals(storageStatus) && !isExpired();
    }
    
    /**
     * 저장된 내용의 길이 반환
     */
    public int getContentLength() {
        return storedContent != null ? storedContent.length() : 0;
    }
    
    /**
     * 저장된 내용 미리보기 (100자)
     */
    public String getContentPreview() {
        if (storedContent == null || storedContent.length() <= 100) {
            return storedContent;
        }
        return storedContent.substring(0, 97) + "...";
    }
    
    /**
     * ChatGPT 응답 상태 확인
     */
    public boolean isResponsePositive() {
        if (chatgptResponse == null) return false;
        
        String response = chatgptResponse.toLowerCase();
        return response.contains("저장 완료") || 
               response.contains("완료") || 
               response.contains("성공") ||
               response.contains("stored");
    }
    
    /**
     * 메모리 키로부터 정보 추출
     */
    public static ChatGptMemoryLog createFromKey(String memoryKey, String content) {
        // memoryKey 형태: event1_team1_stage1
        String[] parts = memoryKey.split("_");
        
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid memory key format: " + memoryKey);
        }
        
        Integer eventCode = Integer.parseInt(parts[0].replace("event", ""));
        Integer teamCode = Integer.parseInt(parts[1].replace("team", ""));
        Integer stageStep = Integer.parseInt(parts[2].replace("stage", ""));
        
        return ChatGptMemoryLog.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stageStep)
                .memoryKey(memoryKey)
                .storedContent(content)
                .storageStatus("STORED")
                .build();
    }
}
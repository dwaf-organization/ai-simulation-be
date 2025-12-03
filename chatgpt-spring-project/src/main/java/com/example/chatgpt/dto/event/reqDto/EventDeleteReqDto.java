package com.example.chatgpt.dto.event.reqDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDeleteReqDto {
    
    @NotNull(message = "삭제할 행사 목록은 필수입니다.")
    @Size(min = 1, message = "삭제할 행사를 선택해주세요.")
    private List<Integer> events;
    
    /**
     * 중복된 event_code가 있는지 확인
     */
    public boolean hasDuplicateEvents() {
        if (events == null) return false;
        
        Set<Integer> uniqueEvents = new HashSet<>(events);
        return uniqueEvents.size() != events.size();
    }
    
    /**
     * 중복 제거된 고유 event_code 목록 반환
     */
    public List<Integer> getUniqueEvents() {
        if (events == null) return List.of();
        
        return events.stream()
            .distinct()
            .toList();
    }
    
    /**
     * 빈 목록인지 확인
     */
    public boolean isEmpty() {
        return events == null || events.isEmpty();
    }
}
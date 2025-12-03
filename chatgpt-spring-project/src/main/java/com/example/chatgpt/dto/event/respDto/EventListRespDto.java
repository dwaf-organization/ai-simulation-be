package com.example.chatgpt.dto.event.respDto;

import com.example.chatgpt.common.dto.PaginationDto;
import com.example.chatgpt.entity.Event;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventListRespDto {
    
    private List<EventItem> content;
    private PaginationDto pagination;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventItem {
        @JsonProperty("event_code")
        private Integer eventCode;
        
        @JsonProperty("event_name")
        private String eventName;
        
        @JsonProperty("event_at")
        private String eventAt;
        
        @JsonProperty("event_status")
        private Integer eventStatus;
        
        @JsonProperty("team_count")
        private Integer teamCount;
        
        /**
         * Event Entity를 EventItem으로 변환
         */
        public static EventItem from(Event event, Integer teamCount) {
            return EventItem.builder()
                .eventCode(event.getEventCode())
                .eventName(event.getEventName())
                .eventAt(event.getEventAt())
                .eventStatus(event.getEventStatus())
                .teamCount(teamCount)
                .build();
        }
    }
}
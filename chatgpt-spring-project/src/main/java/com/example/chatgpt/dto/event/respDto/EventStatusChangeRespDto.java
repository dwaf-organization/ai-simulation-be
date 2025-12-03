package com.example.chatgpt.dto.event.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusChangeRespDto {
    
    private Integer eventCode;
    private Integer eventStatus;
    
    public static EventStatusChangeRespDto from(Integer eventCode, Integer eventStatus) {
        return EventStatusChangeRespDto.builder()
            .eventCode(eventCode)
            .eventStatus(eventStatus)
            .build();
    }
}
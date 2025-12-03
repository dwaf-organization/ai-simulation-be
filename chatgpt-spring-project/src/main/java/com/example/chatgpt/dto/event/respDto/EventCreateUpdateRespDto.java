package com.example.chatgpt.dto.event.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateUpdateRespDto {
    
    private Integer eventCode;
    
    public static EventCreateUpdateRespDto from(Integer eventCode) {
        return EventCreateUpdateRespDto.builder()
            .eventCode(eventCode)
            .build();
    }
}
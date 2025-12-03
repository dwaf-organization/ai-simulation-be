package com.example.chatgpt.dto.event.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDeleteRespDto {
    
    private List<Integer> eventCode;
    
    public static EventDeleteRespDto from(List<Integer> deletedEventCodes) {
        return EventDeleteRespDto.builder()
            .eventCode(deletedEventCodes)
            .build();
    }
}
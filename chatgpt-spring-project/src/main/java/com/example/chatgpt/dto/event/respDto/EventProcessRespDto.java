package com.example.chatgpt.dto.event.respDto;

import com.example.chatgpt.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventProcessRespDto {
    
    private Integer eventCode;              // 이벤트 코드
    private Integer stageBatchProcess;      // 스테이지 일괄처리 단계
    private Integer summaryViewProcess;     // 요약보기 처리 단계
    
    /**
     * Entity를 DTO로 변환
     */
    public static EventProcessRespDto from(Event event) {
        return EventProcessRespDto.builder()
                .eventCode(event.getEventCode())
                .stageBatchProcess(event.getStageBatchProcess())
                .summaryViewProcess(event.getSummaryViewProcess())
                .build();
    }
}
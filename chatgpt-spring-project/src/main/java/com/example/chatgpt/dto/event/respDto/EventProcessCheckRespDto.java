package com.example.chatgpt.dto.event.respDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventProcessCheckRespDto {
    
    private List<Integer> result;  // [stage_batch_process 비교결과, summary_view_process 비교결과]
    
    /**
     * 스테이지와 프로세스 값들을 비교하여 DTO 생성
     */
    public static EventProcessCheckRespDto from(Integer stage, Integer stageBatchProcess, Integer summaryViewProcess) {
        // stage <= 프로세스값 ? 1 : 0
        int stageBatchResult = (stage <= stageBatchProcess) ? 1 : 0;
        int summaryViewResult = (stage <= summaryViewProcess) ? 1 : 0;
        
        return EventProcessCheckRespDto.builder()
                .result(Arrays.asList(stageBatchResult, summaryViewResult))
                .build();
    }
}
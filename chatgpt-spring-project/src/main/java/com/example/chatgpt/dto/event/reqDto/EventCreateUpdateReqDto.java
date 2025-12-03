package com.example.chatgpt.dto.event.reqDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateUpdateReqDto {
    
    private Integer eventCode; // null이면 생성, 값이 있으면 수정
    
    @NotBlank(message = "행사명은 필수입니다.")
    @Size(max = 250, message = "행사명은 최대 250자까지 입력 가능합니다.")
    private String eventName;
    
    @NotBlank(message = "행사날짜는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "행사날짜는 YYYY-MM-DD 형식이어야 합니다.")
    private String eventAt;
    
    /**
     * 날짜 형식 검증 및 변환
     */
    public boolean isValidDateFormat() {
        try {
            LocalDate.parse(eventAt);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 생성 요청인지 확인
     */
    public boolean isCreateRequest() {
        return eventCode == null;
    }
    
    /**
     * 수정 요청인지 확인
     */
    public boolean isUpdateRequest() {
        return eventCode != null;
    }
}
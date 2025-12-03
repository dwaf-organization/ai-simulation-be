package com.example.chatgpt.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    
    private Integer currentPage;
    private Integer totalPage;
    private Integer size;
    private Boolean hasNext;
    
    /**
     * Spring Data Page 객체를 PaginationDto로 변환
     */
    public static PaginationDto from(Page<?> page) {
        return PaginationDto.builder()
            .currentPage(page.getNumber() + 1) // 0-based → 1-based
            .totalPage(page.getTotalPages())
            .size((int) page.getTotalElements())
            .hasNext(page.hasNext())
            .build();
    }
}
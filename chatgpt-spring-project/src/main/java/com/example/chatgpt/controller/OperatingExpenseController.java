package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.service.OperatingExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OperatingExpenseController {

    private final OperatingExpenseService operatingExpenseService;

    /**
     * 지출 조회 API
     */
    @GetMapping("/api/expense/view/{eventCode}/{teamCode}/{stage}")
    public ResponseEntity<RespDto<List<Map<String, Object>>>> getExpenses(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stage") Integer stage) {
        
        try {
            log.info("지출 조회 요청 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stage);
            
            List<Map<String, Object>> expenses = operatingExpenseService.getExpenses(eventCode, teamCode, stage);
            
            return ResponseEntity.ok(RespDto.success("지출 조회 성공", expenses));
            
        } catch (RuntimeException e) {
            log.warn("지출 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("지출 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("지출 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 지출 업데이트 API
     */
    @PutMapping("/api/expense/update")
    public ResponseEntity<RespDto<String>> updateExpenses(
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> expenses = (List<Map<String, Object>>) request.get("expenses");
            
            if (expenses == null || expenses.isEmpty()) {
                return ResponseEntity.ok(RespDto.fail("지출 데이터가 없습니다."));
            }
            
            log.info("지출 업데이트 요청 - {} 개 항목", expenses.size());
            
            operatingExpenseService.updateExpenses(expenses);
            
            return ResponseEntity.ok(RespDto.success("지출 업데이트 완료", "총 " + expenses.size() + "개 항목이 업데이트되었습니다."));
            
        } catch (RuntimeException e) {
            log.warn("지출 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("지출 업데이트 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("지출 업데이트 중 오류가 발생했습니다."));
        }
    }
}
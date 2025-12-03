package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.bank.respDto.BankListRespDto;
import com.example.chatgpt.service.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BankController {
    
    private final BankService bankService;
    
    /**
     * 은행 목록 조회 API
     * GET /api/v1/bank/list
     */
    @GetMapping("/api/v1/bank/list")
    public ResponseEntity<RespDto<List<BankListRespDto>>> getBankList() {
        
        try {
            log.info("은행 목록 조회 요청");
            
            List<BankListRespDto> bankList = bankService.getBankList();
            
            return ResponseEntity.ok(RespDto.success("은행 목록 조회 성공", bankList));
            
        } catch (RuntimeException e) {
            log.warn("은행 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("은행 목록 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("은행 목록 조회 중 오류가 발생했습니다."));
        }
    }
}
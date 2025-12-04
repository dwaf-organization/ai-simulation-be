package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.loaninfo.reqDto.LoanInfoCreateReqDto;
import com.example.chatgpt.dto.loaninfo.respDto.LoanInfoDto;
import com.example.chatgpt.dto.loaninfo.respDto.LoanInfoListRespDto;
import com.example.chatgpt.service.LoanInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanInfoController {
    
    private final LoanInfoService loanInfoService;
    
    /**
     * 대출정보 생성 API
     * POST /api/v1/loan-info/create
     */
    @PostMapping("/api/v1/loan-info/create")
    public ResponseEntity<RespDto<Integer>> createLoanInfo(
            @RequestBody LoanInfoCreateReqDto request) {
        
        try {
            log.info("대출정보 생성 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     request.getEventCode(), request.getTeamCode(), request.getStageStep());
            
            Integer loanCode = loanInfoService.createOrUpdateLoanInfo(request);
            
            return ResponseEntity.ok(RespDto.success("대출정보 등록 완료", loanCode));
            
        } catch (RuntimeException e) {
            log.warn("대출정보 생성 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("대출정보 생성 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("대출정보 등록 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 대출정보 목록 조회 API
     * GET /api/v1/loan-info/list?eventCode=1&teamCode=5&stageStep=6
     */
    @GetMapping("/api/v1/loan-info/list")
    public ResponseEntity<RespDto<List<LoanInfoDto>>> getLoanInfoList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode,
            @RequestParam("stageStep") Integer stageStep) {
        
        try {
            log.info("대출정보 목록 조회 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            LoanInfoListRespDto result = loanInfoService.getLoanInfoList(eventCode, teamCode, stageStep);
            
            if (result.getLoanInfos().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("대출정보가 없습니다.", result.getLoanInfos()));
            } else {
                return ResponseEntity.ok(RespDto.success("대출정보 조회 성공", result.getLoanInfos()));
            }
            
        } catch (RuntimeException e) {
            log.warn("대출정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("대출정보 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("대출정보 조회 중 오류가 발생했습니다."));
        }
    }
}
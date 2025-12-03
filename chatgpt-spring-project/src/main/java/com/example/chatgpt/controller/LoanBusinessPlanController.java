package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.loanbusinessplan.reqDto.LoanBusinessPlanCreateReqDto;
import com.example.chatgpt.dto.loanbusinessplan.respDto.LoanAmountViewRespDto;
import com.example.chatgpt.service.LoanBusinessPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanBusinessPlanController {
    
    private final LoanBusinessPlanService loanBusinessPlanService;
    
    /**
     * 대출산정금액 조회 API
     * GET /api/v1/loan-business-plan/amount/{eventCode}/{teamCode}/{stageStep}
     */
    @GetMapping("/api/v1/loan-business-plan/amount/{eventCode}/{teamCode}/{stageStep}")
    public ResponseEntity<RespDto<LoanAmountViewRespDto>> getLoanAmount(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stageStep") Integer stageStep) {
        
        try {
            log.info("대출산정금액 조회 요청 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep);
            
            LoanAmountViewRespDto result = loanBusinessPlanService.getLoanAmount(eventCode, teamCode, stageStep);
            
            return ResponseEntity.ok(RespDto.success("대출산정금액 조회 성공", result));
            
        } catch (RuntimeException e) {
            log.warn("대출산정금액 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("대출산정금액 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("대출산정금액 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 대출 사업계획서 생성 및 대출금액 산정 API
     * POST /api/v1/loan-business-plan/create
     */
    @PostMapping("/api/v1/loan-business-plan/create")
    public ResponseEntity<RespDto<Integer>> createLoanBusinessPlan(
            @RequestBody LoanBusinessPlanCreateReqDto request) {
        
        try {
            log.info("대출 사업계획서 생성 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     request.getEventCode(), request.getTeamCode(), request.getStageStep());
            
            Integer calculatedLoanAmount = loanBusinessPlanService.createLoanBusinessPlan(request);
            
            // 산정 실패시 (0원) 특별 메시지 처리
            if (calculatedLoanAmount == 0) {
                return ResponseEntity.ok(RespDto.success("대출금 산정에 실패했습니다.", calculatedLoanAmount));
            }
            
            return ResponseEntity.ok(RespDto.success("대출 사업계획서 등록 및 대출금액 산정 완료", calculatedLoanAmount));
            
        } catch (RuntimeException e) {
            log.warn("대출 사업계획서 처리 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("대출 사업계획서 처리 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("대출 사업계획서 처리 중 오류가 발생했습니다."));
        }
    }
}
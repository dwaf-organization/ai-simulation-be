package com.example.chatgpt.controller;

import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.dto.financialstatement.respDto.FinancialStatementViewRespDto;
import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.service.FinancialStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/financial-statement")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FinancialStatementController {
    
    private final FinancialStatementService financialStatementService;
    
    /**
     * 재무제표 조회 API - 새로 추가
     * GET /api/v1/financial-statement/view/{eventCode}/{teamCode}/{stageStep}
     */
    @GetMapping("/view/{eventCode}/{teamCode}/{stageStep}")
    public ResponseEntity<RespDto<FinancialStatementViewRespDto>> getFinancialStatementView(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stageStep") Integer stageStep) {
        
        try {
            log.info("재무제표 조회 요청 - eventCode: {}, teamCode: {}, stageStep: {}", eventCode, teamCode, stageStep);
            
            FinancialStatementViewRespDto result = financialStatementService.getFinancialStatementView(eventCode, teamCode, stageStep);
            
            return ResponseEntity.ok(RespDto.success("재무제표 조회 성공", result));
            
        } catch (RuntimeException e) {
            log.warn("재무제표 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("재무제표 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("재무제표 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * Stage 완료 시 재무제표 업데이트
     */
    @PostMapping("/stage/{stage}/update")
    public ResponseEntity<Map<String, Object>> updateFinancialStatement(
            @PathVariable("stage") Integer stage,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 요청 데이터 추출
            Integer teamCode = (Integer) request.get("teamCode");
            Integer eventCode = (Integer) request.get("eventCode");
            String businessPlan = (String) request.get("businessPlan");
            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
            List<Map<String, Object>> userExpenseInputs = (List<Map<String, Object>>) request.get("userExpenseInputs");
            
            log.info("재무제표 업데이트 요청 - teamCode: {}, stage: {}", teamCode, stage);
            
            // 재무제표 업데이트
            FinancialStatement financialStatement = financialStatementService.updateFinancialStatement(
                teamCode, eventCode, stage, businessPlan, stageAnswers, userExpenseInputs);
            
            response.put("success", true);
            response.put("message", "Stage " + stage + " 재무제표 업데이트 완료");
            response.put("data", formatFinancialStatementResponse(financialStatement));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재무제표 업데이트 실패", e);
            response.put("success", false);
            response.put("message", "재무제표 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            response.put("errorType", "FINANCIAL_UPDATE_ERROR");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 팀의 모든 재무제표 조회
     */
    @GetMapping("/team/{teamCode}")
    public ResponseEntity<Map<String, Object>> getTeamFinancialStatements(@PathVariable("teamCode") Integer teamCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<FinancialStatement> financialStatements = financialStatementService.getTeamFinancialStatements(teamCode);
            
            response.put("success", true);
            response.put("data", financialStatements.stream()
                .map(this::formatFinancialStatementResponse)
                .toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재무제표 조회 실패", e);
            response.put("success", false);
            response.put("message", "재무제표 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 특정 스테이지 재무제표 조회
     */
    @GetMapping("/team/{teamCode}/stage/{stage}")
    public ResponseEntity<Map<String, Object>> getFinancialStatement(
            @PathVariable("teamCode") Integer teamCode,
            @PathVariable("stage") Integer stage) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            var financialStatement = financialStatementService.getFinancialStatement(teamCode, stage);
            
            if (financialStatement.isPresent()) {
                response.put("success", true);
                response.put("data", formatFinancialStatementResponse(financialStatement.get()));
            } else {
                response.put("success", false);
                response.put("message", "해당 스테이지의 재무제표를 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재무제표 조회 실패", e);
            response.put("success", false);
            response.put("message", "재무제표 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 재무제표 응답 포맷 정리
     */
    private Map<String, Object> formatFinancialStatementResponse(FinancialStatement fs) {
        Map<String, Object> formatted = new HashMap<>();
        
        // 기본 정보
        formatted.put("teamCode", fs.getTeamCode());
        formatted.put("stageStep", fs.getStageStep());
        formatted.put("fsScore", fs.getFsScore());
        
        // 재무상태표 (Balance Sheet)
        Map<String, Object> balanceSheet = new HashMap<>();
        
        // 자산
        Map<String, Object> assets = new HashMap<>();
        assets.put("cashAndDeposits", formatAmount(fs.getCashAndDeposits()));
        assets.put("tangibleAssets", formatAmount(fs.getTangibleAssets()));
        assets.put("intangibleAssets", formatAmount(fs.getIntangibleAssets()));
        assets.put("inventoryAssets", formatAmount(fs.getInventoryAssets()));
        assets.put("totalAssets", formatAmount(fs.getTotalAssets()));
        balanceSheet.put("assets", assets);
        
        // 부채
        Map<String, Object> liabilities = new HashMap<>();
        liabilities.put("accountsPayable", formatAmount(fs.getAccountsPayable()));
        liabilities.put("borrowings", formatAmount(fs.getBorrowings()));
        balanceSheet.put("liabilities", liabilities);
        
        // 자본
        Map<String, Object> equity = new HashMap<>();
        equity.put("capitalStock", formatAmount(fs.getCapitalStock()));
        equity.put("totalLiabilitiesEquity", formatAmount(fs.getTotalLiabilitiesEquity()));
        balanceSheet.put("equity", equity);
        
        formatted.put("balanceSheet", balanceSheet);
        
        // 손익계산서 (Income Statement)
        Map<String, Object> incomeStatement = new HashMap<>();
        incomeStatement.put("revenue", formatAmount(fs.getRevenue()));
        incomeStatement.put("cogs", formatAmount(fs.getCogs()));
        incomeStatement.put("grossProfit", formatAmount(fs.getGrossProfit()));
        incomeStatement.put("sgnaExpenses", formatAmount(fs.getSgnaExpenses()));
        incomeStatement.put("rndExpenses", formatAmount(fs.getRndExpenses()));
        incomeStatement.put("operatingIncome", formatAmount(fs.getOperatingIncome()));
        incomeStatement.put("nonOperatingIncome", formatAmount(fs.getNonOperatingIncome()));
        incomeStatement.put("corporateTax", formatAmount(fs.getCorporateTax()));
        incomeStatement.put("netIncome", formatAmount(fs.getNetIncome()));
        
        formatted.put("incomeStatement", incomeStatement);
        
        // 시간 정보
        formatted.put("createdAt", fs.getCreatedAt());
        formatted.put("updatedAt", fs.getUpdatedAt());
        
        return formatted;
    }
    
    /**
     * 금액 포맷팅 (만원 → 원 변환 및 천단위 구분)
     */
    private Map<String, Object> formatAmount(Integer amount) {
        if (amount == null) amount = 0;
        
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("raw", amount); // 원본 값 (만원 단위)
        formatted.put("won", amount * 10000); // 원 단위
        formatted.put("display", String.format("%,d만원", amount)); // 화면 표시용
        formatted.put("displayWon", String.format("%,d원", amount * 10000)); // 원 단위 표시
        
        return formatted;
    }
}
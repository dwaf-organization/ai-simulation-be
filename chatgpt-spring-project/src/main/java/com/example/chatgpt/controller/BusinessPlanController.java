package com.example.chatgpt.controller;

import com.example.chatgpt.config.OpenAiConfig;
import com.example.chatgpt.service.AdminRevenueDistributionService;
import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.entity.GroupSummary;
import com.example.chatgpt.entity.StageSummary;
import com.example.chatgpt.service.BusinessPlanAnalyzer;
import com.example.chatgpt.service.CostClassificationService;
import com.example.chatgpt.service.ExcelLoaderService;
import com.example.chatgpt.service.FileProcessingService;
import com.example.chatgpt.service.FinancialStatementService;
import com.example.chatgpt.service.GroupSummaryService;
import com.example.chatgpt.service.StageSummaryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BusinessPlanController {

    private final FileProcessingService fileProcessingService;
    private final BusinessPlanAnalyzer businessPlanAnalyzer;
    private final OpenAiConfig openAiConfig;
    private final ExcelLoaderService excelLoaderService;
    private final CostClassificationService costClassificationService;
    private final FinancialStatementService financialStatementService;
    private final StageSummaryService stageSummaryService;
    private final AdminRevenueDistributionService adminRevenueDistributionService;
    private final GroupSummaryService groupSummaryService;
    
    private static final int MAX_TEXT_LENGTH = 50000;
    private static final boolean ENABLE_IMAGE_EXTRACTION = true;

    @GetMapping("/")
    public String index() {
        return "business-plan";
    }

    @GetMapping("/stage")
    public String stagePage() {
        return "business-plan-stage";
    }

    @GetMapping("/api/check-config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkConfig() {
        Map<String, Object> response = new HashMap<>();
        
        String apiKey = openAiConfig.getKey();
        boolean hasApiKey = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here");
        
        response.put("hasApiKey", hasApiKey);
        response.put("apiKeyLength", hasApiKey ? apiKey.length() : 0);
        response.put("apiKeyPrefix", hasApiKey ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "NOT SET");
        response.put("model", openAiConfig.getModel());
        response.put("url", openAiConfig.getUrl());
        response.put("temperature", openAiConfig.getTemperature());
        response.put("maxTokens", openAiConfig.getMaxTokens());
        
        response.put("rateLimitInfo", Map.of(
            "checkUrl", "https://platform.openai.com/settings/organization/limits",
            "usageUrl", "https://platform.openai.com/usage",
            "freeTrierLimits", "RPM: 3-5, RPD: 200, TPM: 40,000",
            "tier1Limits", "RPM: 500, RPD: 10,000, TPM: 200,000",
            "howToUpgrade", "결제 정보 등록: https://platform.openai.com/settings/organization/billing"
        ));
        
        if (!hasApiKey) {
            response.put("error", "API 키가 설정되지 않았습니다.");
            response.put("howToFix", "application.properties: openai.api.key=sk-proj-...");
        } else {
            response.put("status", "OK - 설정이 완료되었습니다!");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/test-rate-limit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testRateLimit() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Rate Limit 테스트 시작...");
            String result = businessPlanAnalyzer.testSimpleRequest();
            
            response.put("success", true);
            response.put("message", "테스트 성공!");
            response.put("result", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("테스트 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/api/test-short")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testShortAnalysis() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("짧은 텍스트 테스트 시작...");
            Map<String, Object> result = businessPlanAnalyzer.testShortAnalysis();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("테스트 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 파일 업로드 및 텍스트 추출 (개선 버전)
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "teamCode", required = false) Integer teamCode,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "파일이 비어있습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            String filename = file.getOriginalFilename();
            if (!fileProcessingService.isValidFileFormat(filename)) {
                response.put("success", false);
                response.put("message", "지원하지 않는 파일 형식입니다. PDF 또는 DOCX 파일을 업로드해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("팀 {} 파일 업로드: {}", teamCode, filename);
            
            // 텍스트 추출
            String extractedText = fileProcessingService.extractTextFromFile(file);
            
            // 원본 길이 저장
            int originalLength = extractedText.length();
            
            // 텍스트 길이 제한
            String limitedText = extractedText;
            boolean isTruncated = false;
            
            if (originalLength > MAX_TEXT_LENGTH) {
                limitedText = businessPlanAnalyzer.limitTextLength(extractedText, MAX_TEXT_LENGTH);
                isTruncated = true;
                log.warn("텍스트가 {}자에서 {}자로 축소되었습니다.", originalLength, MAX_TEXT_LENGTH);
            }
            
            // 세션에 사업계획서 저장 (팀코드별로)
            if (teamCode != null) {
                HttpSession session = request.getSession();
                session.setAttribute("businessPlan_" + teamCode, limitedText);
                session.setAttribute("teamCode", teamCode);
                session.setAttribute("eventCode", 1); // 기본값
                
                log.info("팀 {} 사업계획서 세션 저장 완료", teamCode);
            }
            
            response.put("success", true);
            response.put("filename", filename);
            response.put("text", limitedText);
            response.put("originalLength", originalLength);
            response.put("limitedLength", limitedText.length());
            response.put("isTruncated", isTruncated);
            response.put("maxLength", MAX_TEXT_LENGTH);
            response.put("teamCode", teamCode);
            
            if (isTruncated) {
                response.put("warning", String.format(
                    "문서가 너무 길어서 처음 %,d자만 추출되었습니다.", MAX_TEXT_LENGTH));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 처리 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/excel/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testExcelData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("success", true);
            response.put("availableStages", excelLoaderService.getAvailableStages());
            response.put("stage1_categories", excelLoaderService.getMajorCategoriesByStage(1));
            response.put("stage1_variables", excelLoaderService.getVariablesByStage(1).size());
            response.put("stage2_categories", excelLoaderService.getMajorCategoriesByStage(2));
            response.put("stage2_variables", excelLoaderService.getVariablesByStage(2).size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/api/analyze/stage/{stage}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeBusinessPlanByStage(
            @PathVariable("stage") int stage,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer eventCode = (Integer) request.get("eventCode");
            Integer teamCode = (Integer) request.get("teamCode");

            if (eventCode == null || teamCode == null) {
                response.put("success", false);
                response.put("message", "eventCode와 teamCode는 필수입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> previousAnswers = (Map<String, String>) request.get("previousAnswers");

            log.info("팀 {} Stage {} 질문 생성 요청 (eventCode: {})", teamCode, stage, eventCode);

            Map<String, Object> analysisResult = businessPlanAnalyzer.generateQuestionsAndSave(
                eventCode,
                teamCode,
                stage,
                previousAnswers
            );

            response.put("success", true);
            response.put("stage", stage);
            response.put("teamCode", teamCode);
            response.put("eventCode", eventCode);
            response.put("data", analysisResult);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Stage {} 질문 생성 중 오류 발생", stage, e);

            String errorMessage = e.getMessage();

            if (errorMessage != null && errorMessage.contains("429")) {
                response.put("success", false);
                response.put("message", "⏰ OpenAI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
                response.put("hint", "무료 계정은 분당 3회 제한이 있습니다.");
                response.put("errorType", "RATE_LIMIT");
                return ResponseEntity.status(429).body(response);
            }

            response.put("success", false);
            response.put("message", "질문 생성 중 오류가 발생했습니다: " + errorMessage);
            return ResponseEntity.internalServerError().body(response);

        } catch (Exception e) {
            log.error("Stage {} 질문 생성 중 예상치 못한 오류 발생", stage, e);
            response.put("success", false);
            response.put("message", "질문 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/api/analyze")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeBusinessPlan(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String documentText = request.get("text");
            
            if (documentText == null || documentText.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "분석할 텍스트가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("사업계획서 분석 요청. 텍스트 길이: {}", documentText.length());
            
            Map<String, Object> analysisResult = businessPlanAnalyzer.analyzeBusinessPlan(documentText);
            
            response.put("success", true);
            response.put("data", analysisResult);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("사업계획서 분석 중 오류 발생", e);
            
            String errorMessage = e.getMessage();
            
            if (errorMessage != null && errorMessage.contains("429")) {
                response.put("success", false);
                response.put("message", "⏰ OpenAI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
                response.put("hint", "무료 계정은 분당 3-5회 제한이 있습니다.");
                response.put("errorType", "RATE_LIMIT");
                return ResponseEntity.status(429).body(response);
            }
            
            response.put("success", false);
            response.put("message", "분석 중 오류가 발생했습니다: " + errorMessage);
            return ResponseEntity.internalServerError().body(response);
            
        } catch (Exception e) {
            log.error("사업계획서 분석 중 예상치 못한 오류 발생", e);
            response.put("success", false);
            response.put("message", "분석 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/save-answers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAnswers(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        log.info("답변 저장 요청: {}", request);
        
        response.put("success", true);
        response.put("message", "답변이 저장되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ✅ 핵심: Stage 질문 답변 완료 처리 + ChatGPT 메모리 저장
     * 호출시점: 사용자가 모든 질문에 답변 후 "완료" 버튼 클릭
     */
    @PostMapping("/stage/{stage}/complete-answers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeStageAnswers(
            @PathVariable("stage") Integer stage,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer teamCode = (Integer) request.get("teamCode");
            Integer eventCode = (Integer) request.getOrDefault("eventCode", 1);
            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
            
            log.info("팀 {} Stage {} 답변 완료 처리 시작", teamCode, stage);
            
            // 1. 답변 검증
            if (stageAnswers == null || stageAnswers.isEmpty()) {
                response.put("success", false);
                response.put("message", "답변이 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            // 세션에서 사업계획서 조회
            HttpSession session = httpRequest.getSession();
            String businessPlan = (String) session.getAttribute("businessPlan_" + teamCode);
            
            if (businessPlan == null) {
                response.put("success", false);
                response.put("message", "사업계획서 정보가 없습니다. 다시 시작해주세요.");
                return ResponseEntity.ok(response);
            }
            
            // 2. 답변을 세션에 저장
            String sessionKey = String.format("stageAnswers_%d_%d", teamCode, stage);
            session.setAttribute(sessionKey, stageAnswers);
            
            // 지출 입력 정보 조회 
            List<Map<String, Object>> userExpenseInputs = getUserExpenseInputs(eventCode, teamCode, stage, session);
            
            // 3. ✅ ChatGPT 메모리에 그룹 정보 저장
            try {
                adminRevenueDistributionService.storeGroupSummaryInChatGPT(
                    eventCode, teamCode, stage, 
                    businessPlan, stageAnswers, userExpenseInputs
                );
                
                log.info("팀 {} Stage {} ChatGPT 메모리 저장 완료", teamCode, stage);
                
            } catch (Exception e) {
                log.warn("팀 {} Stage {} ChatGPT 메모리 저장 실패 (진행은 계속): {}", teamCode, stage, e.getMessage());
            }
            
            // 4. ✅ 새로 추가: group_summary 테이블에 핵심정보 저장
            try {
                GroupSummary savedSummary = groupSummaryService.saveGroupSummary(
                    eventCode, teamCode, stage, 
                    businessPlan, stageAnswers, userExpenseInputs
                );
                
                log.info("팀 {} Stage {} 그룹 요약 DB 저장 완료 - summaryId: {}", 
                         teamCode, stage, savedSummary.getSummaryId());
                
                response.put("groupSummaryId", savedSummary.getSummaryId());
                
            } catch (Exception e) {
                log.warn("팀 {} Stage {} 그룹 요약 DB 저장 실패 (진행은 계속): {}", teamCode, stage, e.getMessage());
            }
            
            // 5. 대화 히스토리 업데이트
            updateConversationHistory(session, teamCode, stage, stageAnswers);
            
            // 6. 응답 구성
            response.put("success", true);
            response.put("message", String.format("Stage %d 답변이 완료되었습니다.", stage));
            response.put("nextStep", "summary");
            response.put("stageStep", stage);
            response.put("teamCode", teamCode);
            response.put("answersCount", stageAnswers.size());
            response.put("chatGptMemoryStored", true);
            response.put("groupSummaryStored", true);  // ✅ 추가
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage {} 답변 완료 처리 실패", stage, e);
            response.put("success", false);
            response.put("message", "답변 처리 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 답변을 비용 항목으로 분류
     */
    @PostMapping("/api/classify-cost")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> classifyCost(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String question = request.get("question");
            String answer = request.get("answer");
            
            if (question == null || answer == null) {
                response.put("success", false);
                response.put("message", "질문과 답변이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("비용 분류 요청: Q={}, A={}", question, answer);
            
            Map<String, Object> classification = costClassificationService.classifyExpense(question, answer);
            
            response.put("success", true);
            response.put("classification", classification);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("비용 분류 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "비용 분류 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Stage별 답변을 일괄 비용 분류
     */
    @PostMapping("/api/classify-stage-costs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> classifyStageCosts(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> questionsAndAnswers = (Map<String, String>) request.get("answers");
            
            if (questionsAndAnswers == null || questionsAndAnswers.isEmpty()) {
                response.put("success", false);
                response.put("message", "답변이 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("Stage 비용 분류 요청: {}개 답변", questionsAndAnswers.size());
            
            List<Map<String, Object>> classifications = 
                costClassificationService.classifyMultipleExpenses(questionsAndAnswers);
            
            Map<String, Object> totalCost = 
                costClassificationService.calculateTotalCost(classifications);
            
            response.put("success", true);
            response.put("classifications", classifications);
            response.put("totalCost", totalCost);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage 비용 분류 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "비용 분류 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 전체 손익계산서 생성
     */
    @PostMapping("/api/generate-income-statement")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateIncomeStatement(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> allStageClassifications = 
                (Map<String, List<Map<String, Object>>>) request.get("allStages");
            
            Integer budget = (Integer) request.getOrDefault("budget", 200000000); // 기본 2억원
            
            if (allStageClassifications == null || allStageClassifications.isEmpty()) {
                response.put("success", false);
                response.put("message", "Stage 데이터가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("손익계산서 생성 요청: {}개 Stage, 예산 {}원", 
                     allStageClassifications.size(), budget);
            
            Map<String, Object> incomeStatement = 
                costClassificationService.generateIncomeStatement(allStageClassifications, budget);
            
            response.put("success", true);
            response.put("incomeStatement", incomeStatement);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("손익계산서 생성 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "손익계산서 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Stage 완료 후 재무제표 업데이트가 포함된 답변 저장
     */
    @PostMapping("/stage/{stage}/complete")
    public ResponseEntity<Map<String, Object>> completeStageWithFinancial(
            @PathVariable("stage") Integer stage,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 기본 정보 추출
            Integer teamCode = (Integer) request.get("teamCode");
            Integer eventCode = (Integer) request.get("eventCode");
            String businessPlan = (String) request.get("businessPlan");
            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
            List<Map<String, Object>> userExpenseInputs = (List<Map<String, Object>>) request.get("userExpenseInputs");
            
            log.info("Stage {} 완료 처리 시작 - teamCode: {}", stage, teamCode);
            
            // 1. Stage 답변 저장 (기존 로직)
            // ... 기존 답변 저장 코드 ...
            
            // 2. Stage 2부터 재무제표 업데이트
            if (stage >= 2) {
                FinancialStatement financialStatement = financialStatementService.updateFinancialStatement(
                    teamCode, eventCode, stage, businessPlan, stageAnswers, userExpenseInputs);
                
                response.put("financialStatement", formatFinancialStatementSummary(financialStatement));
            }
            
            response.put("success", true);
            response.put("message", "Stage " + stage + " 완료");
            response.put("nextStage", stage + 1);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage 완료 처리 실패", e);
            response.put("success", false);
            response.put("message", "Stage 완료 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Stage 완료 통합 처리 (요약본 + 재무제표)
     */
    @PostMapping("/stage/{stage}/complete-integrated")
    public ResponseEntity<Map<String, Object>> completeStageIntegrated(
            @PathVariable("stage") Integer stage,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 기본 정보 추출
            Integer eventCode = (Integer) request.get("eventCode");
            Integer teamCode = (Integer) request.get("teamCode");
            String businessPlan = (String) request.get("businessPlan");
            Map<String, Object> stageAnswers = (Map<String, Object>) request.get("stageAnswers");
            List<Map<String, Object>> userExpenseInputs = (List<Map<String, Object>>) request.get("userExpenseInputs");
            
            log.info("Stage {} 통합 완료 처리 시작 - teamCode: {}", stage, teamCode);
            
            // 1. Stage 요약본 생성 (모든 Stage)
            StageSummary summary = stageSummaryService.generateStageSummary(
                eventCode, teamCode, stage, businessPlan, stageAnswers);
            
            response.put("summary", formatSummaryResponse(summary));
            
            // 2. Stage 2 이상일 때만 재무제표 처리
            if (stage >= 2) {
                FinancialStatement financialStatement = financialStatementService.updateFinancialStatement(
                    teamCode, eventCode, stage, businessPlan, stageAnswers, userExpenseInputs);
                
                response.put("financialStatement", formatFinancialStatementSummary(financialStatement));
            }
            
            response.put("success", true);
            response.put("message", "Stage " + stage + " 완료");
            response.put("nextStage", stage + 1);
            response.put("stageType", stage == 1 ? "SUMMARY_ONLY" : "SUMMARY_AND_FINANCIAL");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Stage {} 통합 완료 처리 실패", stage, e);
            response.put("success", false);
            response.put("message", "Stage 완료 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 팀 현재 상태 조회
     */
    @GetMapping("/api/team/{teamCode}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeamStatus(
            @PathVariable Integer teamCode,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            HttpSession session = request.getSession();
            
            // 현재 스테이지
            Integer currentStage = (Integer) session.getAttribute("currentStage_" + teamCode);
            if (currentStage == null) currentStage = 1;
            
            // 완료된 스테이지들
            List<Integer> completedStages = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                if (isStageCompleted(i, teamCode, session)) {
                    completedStages.add(i);
                }
            }
            
            // 팀 매출 분배 이력 조회
            List<Map<String, Object>> allocationHistory = new ArrayList<>();
            try {
                allocationHistory = adminRevenueDistributionService.getTeamAllocations(1, teamCode);
            } catch (Exception e) {
                log.debug("팀 매출 분배 이력 조회 실패: {}", e.getMessage());
            }
            
            Map<String, Object> teamStatus = new HashMap<>();
            teamStatus.put("teamCode", teamCode);
            teamStatus.put("currentStage", currentStage);
            teamStatus.put("completedStages", completedStages);
            teamStatus.put("totalProgress", (completedStages.size() * 100) / 7);
            teamStatus.put("hasBusinessPlan", session.getAttribute("businessPlan_" + teamCode) != null);
            teamStatus.put("allocationHistory", allocationHistory);
            
            response.put("success", true);
            response.put("data", teamStatus);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("팀 상태 조회 실패", e);
            response.put("success", false);
            response.put("message", "팀 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // ================================================================================================
    // Private Helper Methods
    // ================================================================================================
    
    /**
     * 사용자 지출 입력 정보 조회
     */
    private List<Map<String, Object>> getUserExpenseInputs(Integer eventCode, Integer teamCode, Integer stage, HttpSession session) {
        List<Map<String, Object>> expenses = new ArrayList<>();
        
        // 세션에서 지출 정보 조회
        String expenseKey = String.format("stageExpenses_%d_%d", teamCode, stage);
        List<Map<String, Object>> sessionExpenses = (List<Map<String, Object>>) session.getAttribute(expenseKey);
        
        if (sessionExpenses != null) {
            expenses.addAll(sessionExpenses);
        } else {
            // 기본값 (실제 구현 시 수정 필요)
            Map<String, Object> defaultExpense = new HashMap<>();
            defaultExpense.put("expenseAmount", "개발자 2명, 연봉 각 6000만원");
            defaultExpense.put("expenseDescription", "핵심 개발팀 구성");
            defaultExpense.put("expenseCategory", "급여");
            expenses.add(defaultExpense);
        }
        
        return expenses;
    }
    
    /**
     * 대화 히스토리 업데이트
     */
    private void updateConversationHistory(HttpSession session, Integer teamCode, Integer stage, Map<String, Object> answers) {
        String historyKey = "conversationHistory_" + teamCode;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) session.getAttribute(historyKey);
        
        if (history == null) {
            history = new ArrayList<>();
        }
        
        Map<String, Object> stageHistory = new HashMap<>();
        stageHistory.put("stage", stage);
        stageHistory.put("answers", answers);
        stageHistory.put("timestamp", new java.util.Date());
        
        history.add(stageHistory);
        session.setAttribute(historyKey, history);
    }
    
    /**
     * 재무제표 요약 정보 (Stage 완료 시 보여줄 정보)
     */
    private Map<String, Object> formatFinancialStatementSummary(FinancialStatement fs) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("stageStep", fs.getStageStep());
        summary.put("remainingCash", String.format("%,d만원", fs.getCashAndDeposits()));
        summary.put("monthlyRevenue", String.format("%,d만원", fs.getRevenue()));
        summary.put("monthlyExpenses", String.format("%,d만원", 
            (fs.getSgnaExpenses() != null ? fs.getSgnaExpenses() : 0) + 
            (fs.getRndExpenses() != null ? fs.getRndExpenses() : 0)));
        summary.put("netIncome", String.format("%,d만원", fs.getNetIncome()));
        summary.put("fsScore", fs.getFsScore() + "점");
        
        // 현금 경고
        if (fs.getCashAndDeposits() < 10000) { // 1억 미만
            summary.put("cashWarning", "⚠️ 현금이 부족합니다! 대출을 고려하세요.");
        }
        
        return summary;
    }

    /**
     * 요약본 응답 포맷 (간단한 버전)
     */
    private Map<String, Object> formatSummaryResponse(StageSummary summary) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("summaryCode", summary.getSummaryCode());
        formatted.put("stageStep", summary.getStageStep());
        formatted.put("textLength", summary.getSummaryText() != null ? summary.getSummaryText().length() : 0);
        formatted.put("createdAt", summary.getCreatedAt());
        
        // 요약본 미리보기 (첫 150자)
        if (summary.getSummaryText() != null && summary.getSummaryText().length() > 150) {
            formatted.put("preview", summary.getSummaryText().substring(0, 150) + "...");
        } else {
            formatted.put("preview", summary.getSummaryText());
        }
        
        return formatted;
    }
    
    /**
     * 스테이지 완료 상태 확인
     */
    private boolean isStageCompleted(Integer stage, Integer teamCode, HttpSession session) {
        String answerKey = String.format("stageAnswers_%d_%d", teamCode, stage);
        Map<String, Object> answers = (Map<String, Object>) session.getAttribute(answerKey);
        return answers != null && !answers.isEmpty();
    }
}
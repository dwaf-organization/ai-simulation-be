package com.example.chatgpt.service;

import com.example.chatgpt.dto.DecisionVariableDto;
import com.example.chatgpt.entity.LlmQuestion;
import com.example.chatgpt.entity.Stage1Bizplan;
import com.example.chatgpt.repository.LlmQuestionRepository;
import com.example.chatgpt.repository.Stage1BizplanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 사업계획서 분석 서비스 (8개 객관식 + 2개 주관식) + DB 저장
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessPlanAnalyzer {

    private final OpenAiService openAiService;
    private final ExcelLoaderService excelLoaderService;
    private final LlmQuestionRepository llmQuestionRepository;
    private final Stage1BizplanRepository stage1BizplanRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ChatGPT API 호출 (범용 메서드)
     */
    public String callChatGptApi(String prompt) {
        try {
            log.debug("ChatGPT API 호출 시작 - 프롬프트 길이: {}자", prompt.length());
            String response = openAiService.chat(prompt);
            log.debug("ChatGPT API 응답 완료 - 응답 길이: {}자", response.length());
            return response;
        } catch (Exception e) {
            log.error("ChatGPT API 호출 실패", e);
            throw new RuntimeException("ChatGPT API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 질문 생성 및 DB 저장 (메인 메서드)
     */
    @Transactional
    public Map<String, Object> generateQuestionsAndSave(
            Integer eventCode,
            Integer teamCode, 
            int stage,
            Map<String, String> previousAnswers) {
        
        log.info("질문 생성 및 저장 시작 - eventCode: {}, teamCode: {}, stage: {}", eventCode, teamCode, stage);
        
        // 1. stage1_bizplan에서 biz_item_summary 조회
        String bizItemSummary = getBizItemSummary(eventCode, teamCode);
        
        if (bizItemSummary == null || bizItemSummary.trim().isEmpty()) {
            throw new RuntimeException("사업계획서 요약이 없습니다. 먼저 사업계획서를 업로드해주세요.");
        }
        
        log.info("사업계획서 요약 조회 완료 - 길이: {}자", bizItemSummary.length());
        
        // 2. 기존 질문이 있으면 삭제 (재생성)
        if (llmQuestionRepository.existsByTeamCodeAndStageStep(teamCode, stage)) {
            log.info("기존 질문 삭제 - teamCode: {}, stage: {}", teamCode, stage);
            llmQuestionRepository.deleteByTeamCodeAndStageStep(teamCode, stage);
        }
        
        // 3. ChatGPT로 질문 생성
        Map<String, Object> questionsResult = analyzeBusinessPlanWithStage(bizItemSummary, stage, previousAnswers);
        
        // 4. DB에 저장
        saveQuestionsToDatabase(eventCode, teamCode, stage, questionsResult);
        
        // 5. 저장된 질문 조회해서 응답
        List<LlmQuestion> savedQuestions = llmQuestionRepository.findByTeamCodeAndStageStep(teamCode, stage);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total_questions", savedQuestions.size());
        result.put("questions", savedQuestions);
        result.put("message", "질문 생성 및 저장 완료");
        
        log.info("질문 생성 및 저장 완료 - 총 {}개 질문", savedQuestions.size());
        
        return result;
    }
    
    /**
     * stage1_bizplan에서 biz_item_summary 조회
     */
    private String getBizItemSummary(Integer eventCode, Integer teamCode) {
        Optional<Stage1Bizplan> bizplanOpt = stage1BizplanRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
        
        if (bizplanOpt.isEmpty()) {
            throw new RuntimeException("사업계획서를 찾을 수 없습니다. (eventCode: " + eventCode + ", teamCode: " + teamCode + ")");
        }
        
        Stage1Bizplan bizplan = bizplanOpt.get();
        String summary = bizplan.getBizItemSummary();
        
        if (summary == null || summary.trim().isEmpty()) {
            // biz_item_summary가 없으면 bizplan_content 사용
            summary = bizplan.getBizplanContent();
            log.warn("biz_item_summary가 없어서 bizplan_content 사용 - teamCode: {}", teamCode);
        }
        
        return summary;
    }
    
    /**
     * 질문들을 DB에 저장
     */
    private void saveQuestionsToDatabase(Integer eventCode, Integer teamCode, int stage, Map<String, Object> questionsResult) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) questionsResult.get("questions");
        
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("생성된 질문이 없습니다.");
        }
        
        log.info("DB 저장 시작 - {}개 질문", questions.size());
        
        for (Map<String, Object> questionData : questions) {
            LlmQuestion llmQuestion = LlmQuestion.builder()
                .eventCode(eventCode)
                .teamCode(teamCode)
                .stageStep(stage)
                .category((String) questionData.get("category"))
                .selectionReason((String) questionData.get("selection_reason"))
                .questionSummary((String) questionData.get("question_summary"))
                .question((String) questionData.get("question"))
                .build();
            
            // 선택지 처리 (객관식만)
            String type = (String) questionData.get("type");
            if ("multiple_choice".equals(type)) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> options = (List<Map<String, String>>) questionData.get("options");
                
                if (options != null && options.size() >= 5) {
                    llmQuestion.setOption1(options.get(0).get("text"));
                    llmQuestion.setOption2(options.get(1).get("text"));
                    llmQuestion.setOption3(options.get(2).get("text"));
                    llmQuestion.setOption4(options.get(3).get("text"));
                    llmQuestion.setOption5(options.get(4).get("text"));
                }
            }
            // 주관식(essay)인 경우 option들은 null로 유지
            
            llmQuestionRepository.save(llmQuestion);
        }
        
        log.info("DB 저장 완료 - {}개 질문", questions.size());
    }

    /**
     * 간단한 테스트 요청 (Rate Limit 확인용)
     */
    public String testSimpleRequest() {
        return openAiService.chat("Say 'Hello'");
    }
    
    /**
     * 짧은 텍스트로 질문 생성 테스트 (TPM 확인용)
     */
    public Map<String, Object> testShortAnalysis() {
        String shortText = "우리는 AI 기반 CRM 서비스를 만듭니다.";
        
        String prompt = "다음 짧은 설명을 보고 질문 1개만 만들어주세요:\n\n" + shortText;
        
        try {
            String response = openAiService.chat(prompt);
            return Map.of("success", true, "result", response);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    /**
     * Stage 기반 사업계획서 분석 (8개 객관식 + 2개 주관식)
     */
    public Map<String, Object> analyzeBusinessPlanWithStage(
            String documentText, 
            int stage,
            Map<String, String> previousAnswers) {
        
        log.info("Stage {} 분석 시작", stage);
        
        // Stage에 해당하는 대분류 목록 가져오기 (코드에서 직접)
        Map<String, List<String>> stageCategories = STAGE_CATEGORIES.get(stage);
        
        if (stageCategories == null || stageCategories.isEmpty()) {
            throw new RuntimeException("Stage " + stage + " 데이터가 없습니다.");
        }
        
        Set<String> majorCategories = stageCategories.keySet();
        log.info("Stage {} 대분류 ({} 개): {}", stage, majorCategories.size(), majorCategories);
        
        // ChatGPT 프롬프트 생성 (엑셀 변수 없이)
        String prompt = createStagePrompt(documentText, stage, null, previousAnswers);
        
        // ChatGPT 호출
        String response = openAiService.chat(prompt);
        
        // 응답 파싱
        return parseQuestionsResponse(response);
    }

    /**
     * Stage별 비즈니스 맥락 정의
     */
    private static final Map<Integer, String> STAGE_CONTEXT = Map.of(
        1, "사업 초기 단계: 팀 구성, MVP 개발, 초기 자금 확보, 시장 진입 준비 단계입니다.",
        2, "초기 운영: 제품/서비스 출시, 초기 고객 확보, 기본 프로세스 확립 단계입니다.",
        3, "성장기 운영: 매출 증대, 조직 확장, 시장 점유율 확대, 비즈니스 모델 검증 단계입니다.",
        4, "안정화 운영: 효율화 추구, 수익성 개선, 시스템 고도화, 지속 가능한 성장 단계입니다.",
        5, "투자 유치 후: 대규모 투자 집행, 빠른 성장 가속화, 시장 선점 전략 단계입니다.",
        6, "글로벌 진출: 해외 시장 진입, 다국적 운영, 글로벌 경쟁력 확보 단계입니다.",
        7, "IPO 준비: 상장 요건 충족, 기업 지배구조 정비, 컴플라이언스 강화 단계입니다."
    );
    
    /**
     * Stage별 우선순위 대분류 및 중분류 정의
     */
    private static final Map<Integer, Map<String, List<String>>> STAGE_CATEGORIES = Map.of(
        1, Map.ofEntries(
            Map.entry("인사/조직", List.of("채용", "조직문화", "조직설계")),
            Map.entry("재무관리", List.of("자금조달", "예산", "회계")),
            Map.entry("제품/서비스", List.of("신제품", "품질", "UX")),
            Map.entry("법무/리스크", List.of("계약", "지적재산", "개인정보")),
            Map.entry("혁신/R&D", List.of("핵심기술", "프로토타입", "특허")),
            Map.entry("마케팅/브랜딩", List.of("브랜드", "시장조사", "콘텐츠")),
            Map.entry("고객관리", List.of("CS", "리뷰", "CRM")),
            Map.entry("IT/인프라", List.of("개발", "클라우드", "시스템")),
            Map.entry("파트너십", List.of("제휴", "생태계")),
            Map.entry("조직문화", List.of("비전", "미션"))
        ),
        2, Map.ofEntries(
            Map.entry("운영효율", List.of("프로세스", "아웃소싱")),
            Map.entry("고객관리", List.of("CS", "CRM")),
            Map.entry("인사/조직", List.of("보상", "근무제도", "평가")),
            Map.entry("재무관리", List.of("회계", "세무", "예산")),
            Map.entry("영업/채널", List.of("영업조직", "영업도구", "B2B")),
            Map.entry("제품/서비스", List.of("품질", "AS", "기능")),
            Map.entry("마케팅/브랜딩", List.of("SNS", "콘텐츠", "커뮤니티")),
            Map.entry("IT/인프라", List.of("보안", "시스템", "데이터")),
            Map.entry("법무/리스크", List.of("컴플라이언스", "계약", "보험")),
            Map.entry("고객경험", List.of("UX", "CS"))
        ),
        3, Map.ofEntries(
            Map.entry("인사/조직", List.of("채용", "교육", "이탈관리")),
            Map.entry("마케팅/브랜딩", List.of("광고", "PR", "이벤트")),
            Map.entry("영업/채널", List.of("영업조직", "유통채널", "영업도구")),
            Map.entry("재무관리", List.of("자금조달", "예산", "자산관리")),
            Map.entry("제품/서비스", List.of("신제품", "라인업", "UX")),
            Map.entry("고객관리", List.of("CRM", "로열티", "리뷰")),
            Map.entry("IT/인프라", List.of("개발", "클라우드", "데이터")),
            Map.entry("공급망", List.of("구매", "협력사", "물류", "재고")),
            Map.entry("운영효율", List.of("생산성", "설비", "재고")),
            Map.entry("혁신/R&D", List.of("기술혁신", "특허", "오픈이노베이션"))
        ),
        4, Map.ofEntries(
            Map.entry("운영효율", List.of("프로세스", "생산성", "아웃소싱")),
            Map.entry("성과관리", List.of("성과평가", "성과관리")),
            Map.entry("재무관리", List.of("재무효율화", "자산관리")),
            Map.entry("인사/조직", List.of("평가", "노무", "조직설계")),
            Map.entry("IT/인프라", List.of("시스템", "자동화", "인프라")),
            Map.entry("고객경험", List.of("UX", "CS")),
            Map.entry("법무/리스크", List.of("컴플라이언스", "개인정보", "보험")),
            Map.entry("공급망", List.of("구매", "협력사", "재고")),
            Map.entry("제품/서비스", List.of("품질", "AS", "기능")),
            Map.entry("위기관리", List.of("리스크평가", "위기대응", "보안"))
        ),
        5, Map.ofEntries(
            Map.entry("재무관리", List.of("투자관리", "재무계획")),
            Map.entry("인사/조직", List.of("채용", "보상")),
            Map.entry("혁신/R&D", List.of("기술투자", "상용화")),
            Map.entry("마케팅/브랜딩", List.of("광고", "PR", "브랜드")),
            Map.entry("영업/채널", List.of("영업조직", "유통채널")),
            Map.entry("IT/인프라", List.of("AI", "데이터", "클라우드")),
            Map.entry("제품/서비스", List.of("신제품", "라인업", "기능")),
            Map.entry("디지털전환", List.of("AI", "디지털전환")),
            Map.entry("파트너십", List.of("투자", "M&A", "생태계")),
            Map.entry("글로벌/확장", List.of("해외진출", "현지화"))
        ),
        6, Map.ofEntries(
            Map.entry("글로벌/확장", List.of("해외진출", "현지화", "물류", "환율")),
            Map.entry("인사/조직", List.of("조직설계", "근무제도")),
            Map.entry("마케팅/브랜딩", List.of("브랜드", "광고", "SNS")),
            Map.entry("영업/채널", List.of("영업조직", "유통채널", "B2B")),
            Map.entry("제품/서비스", List.of("신제품", "품질", "포장")),
            Map.entry("공급망", List.of("구매", "물류", "협력사")),
            Map.entry("IT/인프라", List.of("클라우드", "인프라", "보안")),
            Map.entry("법무/리스크", List.of("컴플라이언스", "계약", "개인정보")),
            Map.entry("재무관리", List.of("환율관리", "글로벌회계")),
            Map.entry("고객관리", List.of("CS", "CRM"))
        ),
        7, Map.ofEntries(
            Map.entry("재무관리", List.of("재무건전성", "회계투명성", "IR", "재무공시")),
            Map.entry("법무/리스크", List.of("컴플라이언스", "지적재산", "소송")),
            Map.entry("이해관계자", List.of("투자자", "주주", "이사회")),
            Map.entry("위험관리", List.of("리스크공시", "위기대응")),
            Map.entry("지속가능성", List.of("ESG경영", "탄소중립", "지속가능보고서")),
            Map.entry("성과관리", List.of("성과평가", "성과관리")),
            Map.entry("인사/조직", List.of("보상", "노무")),
            Map.entry("IT/인프라", List.of("보안", "시스템", "데이터")),
            Map.entry("운영효율", List.of("프로세스", "생산성")),
            Map.entry("고객경험", List.of("CS", "로열티"))
        )
    );
    
    /**
     * Stage 기반 프롬프트 생성 (8개 객관식 + 2개 주관식)
     */
    private String createStagePrompt(
            String documentText,
            int stage,
            List<DecisionVariableDto> variables,
            Map<String, String> previousAnswers) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 사업계획서 평가 전문가입니다.\n\n");
        
        // 스테이지 맥락 추가
        prompt.append("# 현재 비즈니스 단계:\n");
        prompt.append(String.format("**Stage %d - %s**\n\n", stage, STAGE_CONTEXT.getOrDefault(stage, "일반 단계")));
        
        prompt.append("# 사업계획서 내용:\n");
        prompt.append(documentText).append("\n\n");
        
        // 이전 답변이 있으면 포함 (Stage 2 이상)
        if (previousAnswers != null && !previousAnswers.isEmpty()) {
            prompt.append("# 이전 Stage 진행 상황:\n");
            prompt.append(String.format("이 기업은 이미 Stage 1부터 Stage %d까지 총 %d개의 평가를 완료했습니다.\n", 
                stage - 1, 
                previousAnswers.size()
            ));
            prompt.append("이전 Stage에서 평가한 영역들을 고려하되, **구체적인 답변 내용을 반복하지 말고** ");
            prompt.append("사업계획서 내용과 현재 Stage 상황에 맞는 새로운 관점의 질문을 생성하세요.\n\n");
        }
        
        // Stage별 평가 영역 (대분류 > 중분류 목록)
        prompt.append("# 평가 영역 (대분류 > 가능한 중분류):\n");
        Map<String, List<String>> stageCategories = STAGE_CATEGORIES.getOrDefault(stage, Map.of());
        
        int index = 1;
        for (Map.Entry<String, List<String>> entry : stageCategories.entrySet()) {
            String majorCategory = entry.getKey();
            List<String> minorCategories = entry.getValue();
            
            prompt.append(String.format("%d. **%s** > [%s]\n", 
                index++, 
                majorCategory, 
                String.join(", ", minorCategories)
            ));
        }
        
        prompt.append("\n# 요청사항:\n");
        prompt.append(String.format("**Stage %d 상황과 사업계획서를 깊이 분석하여**, **정확히 10개의 질문**을 생성해주세요.\n\n", stage));
        
        prompt.append("## 중분류 선택 및 질문 생성 규칙:\n");
        prompt.append("1. 위 대분류 목록에서 **우선순위가 높은 10개 대분류**를 선택하세요\n");
        prompt.append("2. 선택한 각 대분류에서 사업계획서와 **가장 관련성 높은 중분류 1개**를 선택하세요\n");
        prompt.append("3. 선택 이유를 간단히 명시하세요 (사업계획서의 어떤 부분 때문에 선택했는지)\n");
        prompt.append("4. 사업계획서와 무관한 중분류는 선택하지 마세요\n");
        prompt.append("5. **반드시 정확히 10개의 질문을 생성**하세요 (더 적거나 많으면 안 됩니다)\n\n");
        
        prompt.append("## 질문 생성 규칙:\n");
        prompt.append("1. 선택한 중분류에 대해 질문 1개씩 생성 (총 10개)\n");
        prompt.append("2. **처음 8개**: 객관식 질문 (5개 선택지 A,B,C,D,E)\n");
        prompt.append("3. **마지막 2개**: 주관식 질문 (서술형 답변)\n");
        prompt.append(String.format("4. **Stage %d의 비즈니스 상황**을 반영한 맥락 있는 질문\n", stage));
        prompt.append("5. 해당 중분류의 **핵심 전략적 의사결정**을 다루어야 함\n");
        prompt.append("6. 단순히 '얼마', '몇 개', '몇 명' 같은 정량적 질문 금지\n");
        prompt.append("7. 사업계획서의 구체적인 내용을 반영\n");
        prompt.append("8. 질문은 명확하고 이해하기 쉽게 작성\n\n");
        
        prompt.append("## 선택지 생성 규칙:\n");
        prompt.append("1. **1-8번 질문**: 각각 **5개의 문장형 선택지** 제공 (A,B,C,D,E)\n");
        prompt.append("2. **9-10번 질문**: 선택지 없음 (주관식 서술형)\n");
        prompt.append("3. 선택지는 구체적인 **전략, 방법론, 접근법**을 문장으로 설명\n");
        prompt.append("4. 숫자 범위(예: 1-5억, 10-20명)가 아닌 **전략적 설명 문장**으로 작성\n");
        prompt.append(String.format("5. **Stage %d 상황에 적합한** 현실적인 옵션\n", stage));
        prompt.append("6. 선택지 간에 명확한 차이가 있어야 함\n");
        prompt.append("7. 사업계획서의 특성과 규모에 맞는 선택지\n\n");
        
        // 주관식 질문 예시 추가
        prompt.append("## 주관식 질문 예시 (9-10번):\n");
        prompt.append("✅ **좋은 예**:\n");
        prompt.append("질문: \"귀하의 사업이 직면할 수 있는 가장 큰 리스크 요소와 이에 대한 구체적인 대응 방안을 상세히 설명해주세요.\"\n");
        prompt.append("질문: \"향후 3년 내 글로벌 시장 진출을 위한 단계별 전략과 각 단계별 핵심 성공 요소를 구체적으로 기술해주세요.\"\n\n");
        
        prompt.append("## 출력 형식 (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": 1,\n");
        prompt.append("      \"type\": \"multiple_choice\",\n");
        prompt.append("      \"category\": \"대분류 > 선택한중분류\",\n");
        prompt.append("      \"selection_reason\": \"사업계획서에서 XX 때문에 이 중분류를 선택함\",\n");
        prompt.append("      \"question_summary\": \"질문의 핵심 의도를 3-5단어로 요약\",\n");
        prompt.append("      \"question\": \"객관식 질문 내용\",\n");
        prompt.append("      \"options\": [\n");
        prompt.append("        {\"key\": \"A\", \"text\": \"선택지 A\"},\n");
        prompt.append("        {\"key\": \"B\", \"text\": \"선택지 B\"},\n");
        prompt.append("        {\"key\": \"C\", \"text\": \"선택지 C\"},\n");
        prompt.append("        {\"key\": \"D\", \"text\": \"선택지 D\"},\n");
        prompt.append("        {\"key\": \"E\", \"text\": \"선택지 E\"}\n");
        prompt.append("      ]\n");
        prompt.append("    },\n");
        prompt.append("    ... (1-8번 객관식),\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": 9,\n");
        prompt.append("      \"type\": \"essay\",\n");
        prompt.append("      \"category\": \"대분류 > 선택한중분류\",\n");
        prompt.append("      \"selection_reason\": \"선택 이유\",\n");
        prompt.append("      \"question_summary\": \"질문 핵심 의도\",\n");
        prompt.append("      \"question\": \"주관식 질문 내용 (상세한 서술형 답변 요구)\",\n");
        prompt.append("      \"options\": null\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": 10,\n");
        prompt.append("      \"type\": \"essay\",\n");
        prompt.append("      \"category\": \"대분류 > 선택한중분류\",\n");
        prompt.append("      \"selection_reason\": \"선택 이유\",\n");
        prompt.append("      \"question_summary\": \"질문 핵심 의도\",\n");
        prompt.append("      \"question\": \"주관식 질문 내용 (상세한 서술형 답변 요구)\",\n");
        prompt.append("      \"options\": null\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("**중요**: \n");
        prompt.append("- 반드시 JSON 형식으로만 응답하고, 다른 설명은 추가하지 마세요.\n");
        prompt.append("- **정확히 10개의 질문**을 생성하세요.\n");
        prompt.append("- **1-8번**: type=\"multiple_choice\", options 배열 포함\n");
        prompt.append("- **9-10번**: type=\"essay\", options=null\n");
        prompt.append("- 주관식 질문은 심도 있는 전략적 사고를 요구하는 내용으로 구성하세요.\n");
        
        return prompt.toString();
    }
    
    /**
     * 응답 파싱
     */
    private Map<String, Object> parseQuestionsResponse(String response) {
        try {
            // JSON 추출 (```json ... ``` 형태일 수도 있음)
            String jsonText = response;
            if (response.contains("```json")) {
                jsonText = response.substring(
                    response.indexOf("```json") + 7,
                    response.lastIndexOf("```")
                ).trim();
            } else if (response.contains("```")) {
                jsonText = response.substring(
                    response.indexOf("```") + 3,
                    response.lastIndexOf("```")
                ).trim();
            }
            
            // JSON 완전성 체크
            if (!jsonText.trim().endsWith("}")) {
                log.warn("JSON이 완전하지 않습니다. 응답이 잘린 것으로 보입니다.");
                log.warn("응답 길이: {} 자", response.length());
                throw new RuntimeException(
                    "ChatGPT 응답이 불완전합니다. max_tokens을 증가시키거나 질문 수를 줄여주세요. " +
                    "현재 응답 길이: " + response.length() + "자"
                );
            }
            
            Map<String, Object> result = objectMapper.readValue(jsonText, new TypeReference<>() {});
            log.info("응답 파싱 성공");
            
            // 질문 수 확인
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
            if (questions != null) {
                log.info("생성된 질문 수: {}", questions.size());
                
                // 10개가 아니면 경고
                if (questions.size() != 10) {
                    log.warn("⚠️ 질문이 10개가 아닙니다! 실제: {}개", questions.size());
                }
                
                // 객관식/주관식 비율 확인
                long multipleChoiceCount = questions.stream()
                    .filter(q -> "multiple_choice".equals(q.get("type")))
                    .count();
                long essayCount = questions.stream()
                    .filter(q -> "essay".equals(q.get("type")))
                    .count();
                    
                log.info("객관식: {}개, 주관식: {}개", multipleChoiceCount, essayCount);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("응답 파싱 실패: {}", e.getMessage());
            log.error("원본 응답: {}", response);
            
            // 더 자세한 에러 메시지
            String errorMsg = "ChatGPT 응답 파싱 실패: " + e.getMessage();
            
            if (response.length() > 2000) {
                errorMsg += "\n\n💡 응답이 너무 깁니다 (" + response.length() + "자). " +
                           "max_tokens을 늘리거나 엑셀의 대분류 수를 줄여주세요.";
            }
            
            throw new RuntimeException(errorMsg);
        }
    }
    
    /**
     * 사업계획서를 분석하여 질문과 선택지를 생성 (기존 메서드 - 호환성 유지)
     */
    public Map<String, Object> analyzeBusinessPlan(String documentText) {
        // 기본적으로 Stage 1 사용
        return analyzeBusinessPlanWithStage(documentText, 1, null);
    }

    /**
     * 텍스트 길이 제한 (너무 긴 경우 요약)
     */
    public String limitTextLength(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        log.warn("텍스트가 너무 깁니다. {} -> {} 자로 제한", text.length(), maxLength);
        return text.substring(0, maxLength) + "\n\n... (이하 생략)";
    }
}
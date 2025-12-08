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
    		Map.entry("인사/조직", List.of("초기 팀 구성 전략", "직무 정의서(JD) 설계", "채용 프로세스 구축", "온보딩 매뉴얼 제작", "기본 조직 구조 설계")),
    		Map.entry("재무관리", List.of("초기 운영비 예산 수립", "현금흐름 일정 관리", "비용 통제 체계 설계", "정부지원금/보조금 탐색", "초기 회계 시스템 세팅")),
    		Map.entry("제품/서비스", List.of("MVP 기능 정의", "고객 문제 정의 인터뷰", "서비스 기획서 초안", "UX 기초 와이어프레임", "핵심 가치 제안(Value Proposition) 정립")),
    		Map.entry("법무/리스크", List.of("법인 설립 및 등록 절차", "기본 계약서 템플릿 마련", "개인정보 취급 정책 초안", "지식재산(IP) 확보 전략", "규제 검토 및 리스크 파악")),
    		Map.entry("혁신/R&D", List.of("기술 개발 방향성 설정", "프로토타입 제작 계획", "기술 검증(PoC) 준비", "기술 리서치 자료 조사", "R&D 필요 리소스 도출")),
    		Map.entry("마케팅/브랜딩", List.of("브랜드 핵심 메시지 정의", "시장조사 및 경쟁사 분석", "초기 브랜딩 가이드 개발", "고객 페르소나 설계", "콘텐츠 전략 초안")),
    		Map.entry("고객관리", List.of("VOC 수집 프로세스 구축", "고객 여정 맵 작성", "초기 CS 응대 규칙 설계", "고객 만족도 기준 정의", "CRM 기초 데이터 구조 설계")),
    		Map.entry("IT/인프라", List.of("개발 환경 구축(Git, 서버 등)", "기본 보안 정책 수립", "서비스 아키텍처 초안", "모니터링 도구 선정", "시스템 요구사항 정의")),
    		Map.entry("파트너십", List.of("핵심 협력사 후보 탐색", "공급자/벤더 리스트업", "제휴 가치 제안서 작성", "공동개발 가능성 검토", "파트너 계약 기본 조건 정립")),
    		Map.entry("조직문화", List.of("조직 미션/비전 초안", "사내 커뮤니케이션 원칙 정의", "팀 문화 핵심 키워드 도출", "내부 공유 미팅 구조 설계", "사내 규정(근무·휴가) 기초 구성"))
        ),
        2, Map.ofEntries(
    		Map.entry("인사/조직", List.of("평가 제도 기초 설계(OKR/KPI)", "직급·직책 체계 도입 검토", "역할 및 책임(R&R) 명확화", "채용 기준 고도화(역량 기반)", "조직 진단(팀 성숙도) 수행")),
    		Map.entry("재무관리", List.of("월별 손익(P&L) 분석 구조 확립", "지출결의·자금 집행 프로세스 고도화", "세무 신고 및 절차 체계화", "원가 구조 분석", "재무 대시보드(BI) 구축")),
    		Map.entry("제품/서비스", List.of("제품 로드맵 초안 수립", "기능 우선순위 결정 체계 도입", "UX 테스트 및 개선 주기 구축", "제품 품질 기준 정립", "사용자 행동 데이터 분석 도입")),
    		Map.entry("법무/리스크", List.of("계약서 검토 프로세스 정립", "개인정보 처리 컴플라이언스 점검", "IP(특허·상표) 출원 계획 수립", "리스크 식별 및 대응 매트릭스", "법적 분쟁 예방 정책 수립")),
    		Map.entry("혁신/R&D", List.of("기술 개발 일정·마일스톤 설정", "기술 검증(PoC) 실험 설계", "R&D 문서화 및 기술 기록 체계", "외부 기술 파트너 조사", "기술 난이도 분석 및 리스크 평가")),
    		Map.entry("마케팅/브랜딩", List.of("콘텐츠 제작 프로세스 구축", "SNS·SEO 채널 전략 설계", "브랜딩 요소 고도화(톤앤매너)", "리드 생성 전략(Lead Gen) 도입", "마케팅 퍼널 분석 구조 마련")),
    		Map.entry("고객관리", List.of("NPS(Customer Loyalty) 측정", "CS 응대 시나리오 구축", "고객 세분화(Segmentation) 시작", "이탈원인 분석 프로세스 구축", "CRM 자동화 도입 준비")),
    		Map.entry("IT/인프라", List.of("보안 정책 고도화(로그·권한관리 강화)", "모듈형 아키텍처 도입 검토", "장애 대응 매뉴얼 초안", "배포/릴리즈 자동화 도입(CI/CD)", "데이터 수집·정제 구조 구축")),
    		Map.entry("파트너십", List.of("파트너 성과 평가 기준 수립", "공동 마케팅 협의 구조 설계", "공급망·벤더 안정성 점검", "장기 제휴 조건 가이드라인 작성", "전략적 제휴 로드맵 생성")),
    		Map.entry("조직문화", List.of("회사 핵심 가치(Core Value) 도입", "사내 커뮤니티 프로그램 운영", "피드백 문화 정착 워크숍", "투명한 정보 공유 시스템 구축", "팀 빌딩 프로그램 고도화"))
        ),
        3, Map.ofEntries(
    		Map.entry("인사/조직", List.of("중간관리자 역할 정의", "승진/보상 체계 기초 설계", "직무 전문성 강화 프로그램", "고성과자 관리 전략", "인력 충원 계획 고도화(헤드카운트 플랜)")),
    		Map.entry("재무관리", List.of("예산 대비 실적 분석 정교화", "재무 리스크 분석 프레임 구축", "투자 유치 데이터룸 준비", "장단기 비용 구조 재정립", "제품/프로젝트별 손익 분석")),
    		Map.entry("제품/서비스", List.of("A/B 테스트 운영 고도화", "기능 개선 프로세스 정착", "사용자 행동 데이터 기반 개선", "기술 부채(Tech Debt) 정리", "정식 버전(Release) 기준 수립")),
    		Map.entry("법무/리스크", List.of("서비스 약관 고도화", "IP 포트폴리오 확대 전략", "계약 리스크 평가 체계", "내부 통제 정책 설계", "법적 분쟁 대응 프로토콜")),
    		Map.entry("혁신/R&D", List.of("제품 확장 기술 검토", "R&D 로드맵 세부화", "알고리즘 성능 고도화", "기술 PoC 결과 분석 체계", "외부 연구기관 협력 구축")),
    		Map.entry("마케팅/브랜딩", List.of("브랜드 확장 전략 수립", "성과 기반 캠페인 운영", "고객 페르소나 세분화", "광고 퍼포먼스 데이터 분석", "콘텐츠 자동화 도입")),
    		Map.entry("고객관리", List.of("고객 여정 기반 CX 개선", "고객 이탈 예측 모델 기초", "VOC 데이터 분석 체계", "VIP 프로그램 초기 설계", "고객 커뮤니티 운영")),
    		Map.entry("IT/인프라", List.of("시스템 성능 개선(스케일링)", "인프라 비용 최적화", "API 표준화 및 문서화", "DevOps 체계 고도화", "로그 기반 운영 모니터링")),
    		Map.entry("파트너십", List.of("공동사업 모델 검토", "파트너 성과 보고 체계", "물류·공급 파트너 협력 강화", "공동 R&D 협력 논의", "전략적 파트너 분류 매트릭스")),
    		Map.entry("조직문화", List.of("리더십/관리직 교육 강화", "학습 및 성장 문화 구축", "사내 소통 구조 고도화", "조직 건강성 정기 진단", "성과 공유·축하 문화 정착"))
        ),
        4, Map.ofEntries(
    		Map.entry("인사/조직", List.of("고성과자 유지 전략", "역량 기반 평가 고도화", "인력 재배치·스킬 매핑", "사내 전문가 제도 도입", "조직 재설계(Restructuring) 검토")),
    		Map.entry("재무관리", List.of("비용 효율화 프로젝트 실행", "자금 유동성 강화 전략", "재무 리스크 사전 경보 체계", "프로젝트별 ROI 트래킹", "손익구조 최적화 재정립")),
    		Map.entry("제품/서비스", List.of("제품 운영 안정화 구조 구축", "품질 관리 프로세스 자동화", "기능 개선 사이클 최적화", "사용자 경험(UX) 검증 체계 고도화", "장애/버그 대응 속도 개선")),
    		Map.entry("법무/리스크", List.of("컴플라이언스 체계 강화", "리스크 관리 체계 정착", "보안 인증(ISO 등) 검토", "계약서 표준화 및 자동화", "규제 변화 모니터링 시스템")),
    		Map.entry("혁신/R&D", List.of("기술 상용화 단계 진입", "R&D 성능지표(KR) 강화", "파일럿 테스트 운영 고도화", "기술기획팀 운영 체계 확립", "신기술 검증 보고 체계")),
    		Map.entry("마케팅/브랜딩", List.of("마케팅 자동화 체계 구축", "고객 세그먼트별 전략 고도화", "브랜드 일관성(BI/VI) 강화", "성과 기반 콘텐츠 전략", "마케팅 채널 믹스(Mix) 최적화")),
    		Map.entry("고객관리", List.of("데이터 기반 고객 만족도 개선", "고객 라이프사이클 분석", "CS 대응 매뉴얼 고도화", "고객 유지율(Retention) 강화 전략", "고객 평가 및 피드백 자동화")),
    		Map.entry("IT/인프라", List.of("시스템 모니터링 자동화", "데이터 파이프라인 고도화", "IT 비용 구조 최적화", "아키텍처 성능 개선(Scaling Up)", "보안 침해 대응 체계 강화")),
    		Map.entry("파트너십", List.of("파트너 성과 분석 보고서 정례화", "전략 파트너 확대 및 재조정", "공동 운영 프로세스 확립", "장기 협력 모델 설계", "파트너 계약 자동화 및 표준화")),
    		Map.entry("조직문화", List.of("투명 경영·정보 공개 강화", "사내 리더십 체계 고도화", "문화 지표(Culture KPI) 도입", "구성원 성장 로드맵 정착", "조직 건강성 장기 모니터링"))
        ),
        5, Map.ofEntries(
    		Map.entry("인사/조직", List.of("고급 인재 확보 전략(Headhunting)", "리더십 파이프라인 구축", "직무 전문성 심화 트랙 운영", "인력 계획(HC Plan) 연 1회 정교화", "조직 효율성 지표 도입(OE Index)")),
    		Map.entry("재무관리", List.of("사업 확장 대비 재무 모델링 고도화", "투자 라운드별 자금 운영 전략", "재무 구조 안정화 프로그램", "프로젝트별 CAPEX/OPEX 분석", "기업가치 평가 자료(Valuation Deck) 준비")),
    		Map.entry("제품/서비스", List.of("대규모 트래픽 대비 아키텍처 개선", "제품 운영 자동화 레벨 확장", "글로우업 버전(Enhanced Release) 기획", "NPS 기반 기능 우선순위 재조정", "고급 사용자 행동 분석 모델 적용")),
    		Map.entry("법무/리스크", List.of("투자 계약서/지분 계약 전문 검토", "국제 규제(글로벌 컴플라이언스) 점검", "지식재산 포트폴리오 확장", "법무 리스크 사전 차단 시스템 도입", "파트너/벤더 리스크 평가 정교화")),
    		Map.entry("혁신/R&D", List.of("차세대 기술 로드맵 구축", "알고리즘 고도화(AI/ML 강화)", "기술 상용화 확장 전략", "오픈이노베이션(산·학·연) 본격 참여", "장기 R&D 포트폴리오 운영")),
    		Map.entry("마케팅/브랜딩", List.of("브랜드 확장(Brand Extension) 전략", "마케팅 자동화 레벨2 도입", "옴니채널 브랜드 일관성 강화", "퍼포먼스 최대화 광고 구조 구축", "고객 생애가치(LTV) 기반 전략 설정")),
    		Map.entry("고객관리", List.of("정교한 고객 세그먼트 운영", "고객 예측 분석 모델 고도화", "고객 이탈 사전 알림 시스템 구축", "VIP/프리미엄 고객 정책 확립", "고객센터 운영 자동화 수준 확장")),
    		Map.entry("IT/인프라", List.of("글로벌 서비스 대비 서버 구조 준비", "데이터 거버넌스 체계 구축", "대규모 트래픽 대비 스케일아웃", "보안 레벨 향상(ISMS/ISO 대응)", "MLOps/DevOps 고도화")),
    		Map.entry("파트너십", List.of("글로벌 파트너 발굴 전략", "파트너 공동수익 모델 설계", "공급망 구조 고도화", "장기 계약 관리 체계", "파트너 성과 KPI 도입")),
    		Map.entry("조직문화", List.of("글로벌 조직문화 기초 수립", "사내 혁신 문화 촉진 제도", "데이터 기반 조직 역량 진단", "주도적 업무 문화(Self-driven) 강화", "사내 리더십 사례 공유 플랫폼 운영"))
        ),
        6, Map.ofEntries(
    		Map.entry("인사/조직", List.of("글로벌 인력 운영 체계 구축", "해외 인재 채용 모델 도입", "다국적 팀 협업 프로토콜", "글로벌 리더십 개발 프로그램", "지역별 HR 정책 차별화 운영")),
    		Map.entry("재무관리", List.of("글로벌 회계 기준(IFRS) 대응", "환율 리스크 관리 체계", "해외 법인 손익 통합 관리", "글로벌 조세 전략 수립", "해외 투자비 분석 모델 구축")),
    		Map.entry("제품/서비스", List.of("해외 시장 맞춤형 기능 로컬라이징", "글로벌 품질 기준(QoS) 정착", "서비스 언어 확장(다국어)", "글로벌 트래픽 부하 대응", "해외 고객 행동 데이터 분석")),
    		Map.entry("법무/리스크", List.of("국제법·국가별 규제 대응", "글로벌 개인정보 보호(GDPR 등) 준수", "해외 파트너십 법률 검토", "국가별 계약 표준화", "해외 분쟁 대비 체계 수립")),
    		Map.entry("혁신/R&D", List.of("글로벌 기술 협력 네트워크 구축", "R&D 성과 글로벌 적용 전략", "첨단 기술도입 로드맵", "글로벌 공동 연구 프로젝트", "기술 검증 국제 인증 대응")),
    		Map.entry("마케팅/브랜딩", List.of("글로벌 브랜드 전략 재정립", "국가별 마케팅 채널 최적화", "해외 캠페인 운영 프로세스", "현지 문화 기반 콘텐츠 제작", "글로벌 인지도 상승 전략")),
    		Map.entry("고객관리", List.of("해외 고객 CS센터 운영", "국가별 고객 세그먼트 분석", "글로벌 VOC 데이터 통합", "시차 기반 CS 대응 자동화", "글로벌 고객 피드백 시스템")),
    		Map.entry("IT/인프라", List.of("글로벌 서버·CDN 구조 구축", "국가별 데이터 저장 규정 준수", "글로벌 보안 레벨 대응", "고신뢰 인프라 운영(SRE 확장)", "기술 표준화 및 국제 인증 준비")),
    		Map.entry("파트너십", List.of("해외 유통·물류 파트너 확보", "글로벌 사업 협력 모델 확장", "국가별 벤더 관리 체계", "해외 파트너 KPI 체계", "공동 프로모션(현지) 전략")),
    		Map.entry("조직문화", List.of("글로벌 조직문화 통합 전략", "다양성·포용성(DE&I) 강화", "다국적 팀 간 갈등관리 체계", "글로벌 커뮤니케이션 툴 정착", "문화 적응(Onboarding) 프로그램"))
        ),
        7, Map.ofEntries(
    		Map.entry("인사/조직", List.of("차세대 리더십 승계 계획(Succession Plan)", "조직 성숙도 모델 정착", "글로벌 HRM 통합 관리", "경영진 평판·윤리 기준 강화", "조직 효율성 최적화 운영")),
    		Map.entry("재무관리", List.of("장기 재무 전략(10년 플랜) 수립", "위험 헤지(Hedging) 전략 정착", "투자자 관계(IR) 정기 운영", "기업 재무 투명성 강화 체계", "지속가능 재무 보고(ESG Disclosure)")),
    		Map.entry("제품/서비스", List.of("제품 수명주기(Lifecycle) 관리", "장기 품질 유지 전략", "지속가능 제품 프로세스 도입", "유지보수 비용 최적화", "장기 사용자 데이터 기반 예측 모델")),
    		Map.entry("법무/리스크", List.of("지배구조(Governance) 고도화", "감사·감독 체계 강화", "규제기관 대응 프로세스 확립", "리스크 공시 체계 강화", "법적 분쟁 사후관리 체계")),
    		Map.entry("혁신/R&D", List.of("지속가능 기술 연구 체계", "장기 기술 로드맵 검증", "기술 축적 및 내부 IP 아카이브 운영", "고도화된 알고리즘 지속 개선", "신기술 영향 평가 체계")),
    		Map.entry("마케팅/브랜딩", List.of("브랜드 자산 관리 체계(BE, BI) 구축", "장기 고객 신뢰 형성 전략", "글로벌 브랜드 평판 관리", "ESG 기반 브랜드 전략", "장기 채널 운영 최적화")),
    		Map.entry("고객관리", List.of("충성 고객(Loyalty) 장기 관리", "고객 가치 기반 예측 모델 정착", "글로벌 고객 만족도 장기 리포트", "고객 커뮤니티 생태계 유지", "고객 생애가치(LTV) 장기 개선 프로그램")),
    		Map.entry("IT/인프라", List.of("데이터 거버넌스 장기 체계", "글로벌 보안 인증 유지 관리", "시스템 장기 유지보수 계획", "기술 표준 장기 관리(Architecture Governance)", "인프라 비용 장기 최적화 전략")),
    		Map.entry("파트너십", List.of("글로벌 파트너 생태계 구축", "장기 협력 가치 평가 체계", "주요 파트너 리스크 모니터링", "공동 성장 전략(Co-growth Model)", "파트너 계약 장기 리뉴얼 체계")),
    		Map.entry("조직문화", List.of("ESG 기반 조직문화 정착", "지속가능 문화 프로그램 운영", "경영진 투명성 및 윤리 준수 강화", "구성원·이해관계자 가치 통합", "장기 조직 건강성 지표 관리"))
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
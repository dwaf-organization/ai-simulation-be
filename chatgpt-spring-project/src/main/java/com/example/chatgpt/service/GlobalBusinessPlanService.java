package com.example.chatgpt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 국가별 사업계획서 생성 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GlobalBusinessPlanService {

    private final OpenAiService openAiService;

    /**
     * 국가별 사업계획서 생성
     * 
     * @param country 국가 코드 (USA, CHINA, JAPAN)
     * @param originalText 원본 한국어 사업계획서
     * @param stageAnswers Stage 1-5 답변 내용
     * @return 국가별 형식의 사업계획서
     */
    public String generateGlobalBusinessPlan(String country, String originalText, Map<String, Object> stageAnswers) {
        log.info("국가별 사업계획서 생성 시작: {}", country);
        
        String prompt = createCountrySpecificPrompt(country, originalText, stageAnswers);
        String response = openAiService.chat(prompt);
        
        log.info("국가별 사업계획서 생성 완료: {}", country);
        return response;
    }

    /**
     * 국가별 프롬프트 생성
     */
    private String createCountrySpecificPrompt(String country, String originalText, Map<String, Object> stageAnswers) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 글로벌 비즈니스 전문가입니다.\n\n");
        
        // 국가별 프롬프트
        switch (country) {
            case "USA":
                prompt.append(createUSAPrompt(originalText, stageAnswers));
                break;
            case "CHINA":
                prompt.append(createChinaPrompt(originalText, stageAnswers));
                break;
            case "JAPAN":
                prompt.append(createJapanPrompt(originalText, stageAnswers));
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 국가입니다: " + country);
        }
        
        return prompt.toString();
    }

    /**
     * 미국 형식 프롬프트
     */
    private String createUSAPrompt(String originalText, Map<String, Object> stageAnswers) {
        return "# 🇺🇸 미국 형식 사업계획서 (Business Plan) 생성\n\n" +
               "다음 한국어 사업계획서를 **미국 스타일의 Business Plan**으로 변환해주세요.\n\n" +
               "## 원본 한국 사업계획서:\n" + originalText + "\n\n" +
               "## Stage 1-5 답변 내용:\n" + formatStageAnswers(stageAnswers) + "\n\n" +
               "## 미국 형식 특징 (반드시 준수):\n" +
               "1. **Executive Summary 최우선**: 1-2페이지로 핵심만 간결하게\n" +
               "   - 비즈니스 핵심 가치 제안 (Value Proposition)\n" +
               "   - 타겟 마켓과 기회 (Target Market & Opportunity)\n" +
               "   - 경쟁 우위 (Competitive Advantage)\n" +
               "   - 재무 하이라이트 (Financial Highlights)\n" +
               "   - 투자 요청 금액과 용도 (Funding Ask)\n\n" +
               "2. **Problem-Solution 접근**:\n" +
               "   - 명확한 문제 정의 (Pain Points)\n" +
               "   - 혁신적인 솔루션 제시\n" +
               "   - 왜 지금이 적기인지 (Why Now?)\n\n" +
               "3. **시장 분석 (Market Analysis)**:\n" +
               "   - TAM, SAM, SOM 제시\n" +
               "   - 시장 성장률과 트렌드\n" +
               "   - 경쟁사 분석 (Competitive Landscape)\n\n" +
               "4. **비즈니스 모델 (Business Model)**:\n" +
               "   - 수익 모델 명확히\n" +
               "   - Unit Economics\n" +
               "   - Scalability\n\n" +
               "5. **Go-to-Market Strategy**:\n" +
               "   - Customer Acquisition Strategy\n" +
               "   - Sales & Marketing Plan\n" +
               "   - Partnerships\n\n" +
               "6. **Management Team**:\n" +
               "   - 팀의 전문성과 경험\n" +
               "   - 각 멤버의 역할\n" +
               "   - Advisory Board (있는 경우)\n\n" +
               "7. **Financial Projections** (3-5년):\n" +
               "   - Revenue Forecast\n" +
               "   - EBITDA, Net Income\n" +
               "   - Cash Flow\n" +
               "   - Break-even Analysis\n\n" +
               "8. **Funding Requirements**:\n" +
               "   - 투자 금액\n" +
               "   - 자금 사용 계획 (Use of Funds)\n" +
               "   - Milestones\n" +
               "   - Exit Strategy (IPO, M&A 등)\n\n" +
               "## 스타일 가이드:\n" +
               "- **간결함**: 15-20 페이지 이내\n" +
               "- **데이터 기반**: 숫자와 그래프 활용\n" +
               "- **자신감**: 야심차고 공격적인 톤\n" +
               "- **투자자 중심**: ROI와 성장 잠재력 강조\n" +
               "- **액션 지향적**: 구체적인 실행 계획\n\n" +
               "## 출력 형식:\n" +
               "마크다운 형식으로 작성하되, 명확한 섹션 구분과 불렛 포인트를 사용하세요.\n" +
               "각 섹션은 간결하고 핵심만 담아주세요.";
    }

    /**
     * 중국 형식 프롬프트
     */
    private String createChinaPrompt(String originalText, Map<String, Object> stageAnswers) {
        return "# 🇨🇳 중국 형식 사업계획서 (商业计划书) 생성\n\n" +
               "다음 한국어 사업계획서를 **중국 스타일의 商业计划书**로 변환해주세요.\n\n" +
               "## 원본 한국 사업계획서:\n" + originalText + "\n\n" +
               "## Stage 1-5 답변 내용:\n" + formatStageAnswers(stageAnswers) + "\n\n" +
               "## 중국 형식 특징 (반드시 준수):\n" +
               "1. **项目概述 (프로젝트 개요)**:\n" +
               "   - 项目背景 (프로젝트 배경)\n" +
               "   - 项目简介 (프로젝트 소개)\n" +
               "   - 项目愿景和使命 (비전과 미션)\n\n" +
               "2. **政策支持与市场环境 (정책 지원 및 시장 환경)**:\n" +
               "   - 国家政策支持 (국가 정책 지원) - 매우 중요!\n" +
               "   - 行业政策 (산업 정책)\n" +
               "   - 地方政府支持 (지방 정부 지원)\n" +
               "   - 정책 부합성 강조\n\n" +
               "3. **市场分析 (시장 분석)**:\n" +
               "   - 중국 시장 규모 (구체적 숫자)\n" +
               "   - 中国市场特点 (중국 시장 특성)\n" +
               "   - 目标客户群 (타겟 고객)\n" +
               "   - 市场增长预测 (시장 성장 전망)\n\n" +
               "4. **产品与服务 (제품 및 서비스)**:\n" +
               "   - 产品介绍 (제품 소개)\n" +
               "   - 技术优势 (기술 우위)\n" +
               "   - 本地化策略 (현지화 전략) - 중요!\n\n" +
               "5. **商业模式 (비즈니스 모델)**:\n" +
               "   - 盈利模式 (수익 모델)\n" +
               "   - 价格策略 (가격 전략)\n" +
               "   - 渠道策略 (채널 전략)\n\n" +
               "6. **市场营销策略 (마케팅 전략)**:\n" +
               "   - 线上营销 (온라인 마케팅 - 微信, 抖音, 小红书 등)\n" +
               "   - 线下推广 (오프라인 프로모션)\n" +
               "   - KOL合作 (KOL 협력)\n\n" +
               "7. **合作伙伴关系 (파트너십)**:\n" +
               "   - 政府关系 (정부 관계) - 매우 중요!\n" +
               "   - 战略合作伙伴 (전략적 파트너)\n" +
               "   - 供应商关系 (공급사 관계)\n\n" +
               "8. **团队介绍 (팀 소개)**:\n" +
               "   - 管理团队 (경영진)\n" +
               "   - 核心成员经历 (핵심 멤버 경력 - 정부/대기업 경력 강조)\n" +
               "   - 顾问团队 (자문단)\n\n" +
               "9. **财务计划 (재무 계획)**:\n" +
               "   - 投资需求 (투자 요구)\n" +
               "   - 资金使用计划 (자금 사용 계획)\n" +
               "   - 收入预测 (수익 예측 - 5년)\n" +
               "   - 投资回报分析 (투자 회수 분석)\n\n" +
               "10. **风险分析 (리스크 분석)**:\n" +
               "    - 市场风险 (시장 리스크)\n" +
               "    - 政策风险 (정책 리스크)\n" +
               "    - 竞争风险 (경쟁 리스크)\n" +
               "    - 风险应对措施 (리스크 대응 방안)\n\n" +
               "## 스타일 가이드:\n" +
               "- **상세함**: 30-50 페이지로 구체적으로\n" +
               "- **정부 정책 연계**: 5개년 계획, 산업 정책과의 부합성 강조\n" +
               "- **시장 규모 강조**: 중국 내 거대 시장 잠재력을 구체적 숫자로\n" +
               "- **관계망 중시**: 파트너십, 네트워크, 정부/기관과의 관계\n" +
               "- **현지화**: 중국 시장 특성에 맞는 전략\n\n" +
               "## 출력 형식:\n" +
               "마크다운 형식으로 작성하되, 각 섹션을 상세하게 작성하세요.\n" +
               "정부 정책과의 연계성을 반드시 강조하세요.";
    }

    /**
     * 일본 형식 프롬프트
     */
    private String createJapanPrompt(String originalText, Map<String, Object> stageAnswers) {
        return "# 🇯🇵 일본 형식 사업계획서 (事業計画書) 생성\n\n" +
               "다음 한국어 사업계획서를 **일본 스타일의 事業計画書**로 변환해주세요.\n\n" +
               "## 원본 한국 사업계획서:\n" + originalText + "\n\n" +
               "## Stage 1-5 답변 내용:\n" + formatStageAnswers(stageAnswers) + "\n\n" +
               "## 일본 형식 특징 (반드시 준수):\n" +
               "1. **事業概要 (사업 개요)**:\n" +
               "   - 事業の目的 (사업 목적)\n" +
               "   - 事業の背景 (사업 배경)\n" +
               "   - 事業のビジョン (사업 비전)\n\n" +
               "2. **市場分析 (시장 분석)**:\n" +
               "   - 市場規模と成長性 (시장 규모와 성장성)\n" +
               "   - 顧客ニーズ分析 (고객 니즈 분석)\n" +
               "   - 競合分析 (경쟁사 분석)\n" +
               "   - 市場動向 (시장 동향)\n\n" +
               "3. **商品・サービス (제품/서비스)**:\n" +
               "   - 商品・サービスの概要 (개요)\n" +
               "   - 特徴と強み (특징과 강점)\n" +
               "   - 品質管理体制 (품질 관리 체계) - 매우 중요!\n" +
               "   - 知的財産権 (지적재산권)\n\n" +
               "4. **販売戦略 (판매 전략)**:\n" +
               "   - ターゲット市場 (타겟 시장)\n" +
               "   - 販売チャネル (판매 채널)\n" +
               "   - 価格戦略 (가격 전략)\n" +
               "   - 販売計画 (판매 계획)\n\n" +
               "5. **マーケティング戦略 (마케팅 전략)**:\n" +
               "   - プロモーション計画 (프로모션 계획)\n" +
               "   - 広告戦略 (광고 전략)\n" +
               "   - ブランディング (브랜딩)\n\n" +
               "6. **組織・人員計画 (조직/인원 계획)**:\n" +
               "   - 組織構造 (조직 구조)\n" +
               "   - 経営陣の紹介 (경영진 소개)\n" +
               "   - 人員計画 (인원 계획)\n" +
               "   - 採用計画 (채용 계획)\n\n" +
               "7. **生産・運営計画 (생산/운영 계획)**:\n" +
               "   - 生産体制 (생산 체제)\n" +
               "   - 品質管理 (품질 관리)\n" +
               "   - サプライチェーン (공급망)\n" +
               "   - 運営フロー (운영 플로우)\n\n" +
               "8. **財務計画 (재무 계획)**:\n" +
               "   - 資金調達計画 (자금 조달 계획)\n" +
               "   - 売上計画 (매출 계획 - 3-5년)\n" +
               "   - 損益計算書予測 (손익계산서 예측)\n" +
               "   - 資金繰り計画 (자금 순환 계획)\n" +
               "   - 投資回収計画 (투자 회수 계획)\n\n" +
               "9. **リスク管理 (리스크 관리)** - 매우 중요!:\n" +
               "   - 想定されるリスク (예상 리스크)\n" +
               "   - リスク対策 (리스크 대책)\n" +
               "   - 危機管理体制 (위기 관리 체계)\n" +
               "   - 保険計画 (보험 계획)\n\n" +
               "10. **実行計画 (실행 계획)**:\n" +
               "    - マイルストーン (마일스톤)\n" +
               "    - 実施スケジュール (실시 스케줄)\n" +
               "    - KPI設定 (KPI 설정)\n\n" +
               "## 스타일 가이드:\n" +
               "- **정교함**: 20-30 페이지로 상세하게\n" +
               "- **리스크 관리 강조**: 실패 시나리오와 대응 방안 필수\n" +
               "- **장기적 관점**: 3-5년 중장기 계획\n" +
               "- **신중함**: 보수적이고 현실적인 예측\n" +
               "- **격식**: 공손하고 격식있는 표현\n" +
               "- **완벽주의**: 디테일과 정확성 중시\n\n" +
               "## 출력 형식:\n" +
               "마크다운 형식으로 작성하되, 각 섹션을 정교하고 상세하게 작성하세요.\n" +
               "특히 리스크 관리 부분을 충실히 작성하세요.";
    }

    /**
     * Stage 답변 포맷팅
     */
    private String formatStageAnswers(Map<String, Object> stageAnswers) {
        if (stageAnswers == null || stageAnswers.isEmpty()) {
            return "(Stage 답변 없음)";
        }
        
        StringBuilder sb = new StringBuilder();
        stageAnswers.forEach((stage, answers) -> {
            sb.append("### ").append(stage).append(":\n");
            if (answers instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> answerMap = (Map<String, String>) answers;
                answerMap.forEach((question, answer) -> {
                    sb.append("- **Q**: ").append(question).append("\n");
                    sb.append("  **A**: ").append(answer).append("\n\n");
                });
            }
        });
        return sb.toString();
    }
}
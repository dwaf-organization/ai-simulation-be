package com.example.chatgpt.service;

import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6BizPlanParseRespDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6CountryBizPlanRespDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6CountryBizPlanRespDto.CountryGenerationResult;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6CountryBizPlanViewRespDto;
import com.example.chatgpt.entity.Event;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.entity.Stage6BizplanSummary;
import com.example.chatgpt.repository.EventRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import com.example.chatgpt.repository.Stage6BizplanSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class Stage6BizPlanService {
    
    private final Stage6BizplanSummaryRepository stage6BizplanSummaryRepository;
    private final EventRepository eventRepository;
    private final TeamMstRepository teamMstRepository;
    private final FileProcessingService fileProcessingService;
    private final BusinessPlanAnalyzer businessPlanAnalyzer; // ChatGPT API
    private final GlobalBusinessPlanService globalBusinessPlanService; // 국가별 사업계획서 생성
    
    private static final int MAX_TEXT_LENGTH = 60000; // 60,000자 제한
    
    /**
     * 1단계: 사업계획서 파일 파싱 및 DB 저장
     * @param eventCode 행사코드
     * @param teamCode 팀코드
     * @param file 업로드된 파일 (텍스트 추출용)
     * @param bizplanFilePath Firebase URL
     * @return 파싱된 텍스트 내용
     */
    @Transactional
    public Stage6BizPlanParseRespDto uploadAndParseBizPlan(Integer eventCode, Integer teamCode, 
                                                          MultipartFile file, String bizplanFilePath) {
        log.info("Stage6 사업계획서 업로드 시작 - eventCode: {}, teamCode: {}, fileName: {}", 
                 eventCode, teamCode, file.getOriginalFilename());
        
        try {
            // 1. 행사/팀 존재 여부 확인
            validateEventAndTeam(eventCode, teamCode);
            
            // 2. 파일 형식 검증 및 텍스트 추출
            String parsedContent = extractAndLimitText(file);
            
            // 3. 기존 데이터 확인 (덮어쓰기)
            Optional<Stage6BizplanSummary> existing = stage6BizplanSummaryRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode);
            
            Stage6BizplanSummary bizplanSummary;
            
            if (existing.isPresent()) {
                // 덮어쓰기
                bizplanSummary = existing.get();
                bizplanSummary.setBizplanFilePath(bizplanFilePath);
                bizplanSummary.setBizItemSummary(parsedContent);
                // 글로벌 관련 필드는 유지 (null이면 그대로, 값이 있으면 그대로)
                
                log.info("기존 Stage6 사업계획서 덮어쓰기 - stage6Code: {}", bizplanSummary.getStage6Code());
                
            } else {
                // 새로 생성
                bizplanSummary = Stage6BizplanSummary.builder()
                    .eventCode(eventCode)
                    .teamCode(teamCode)
                    .bizplanFilePath(bizplanFilePath)
                    .bizItemSummary(parsedContent)
                    // 나머지는 null
                    .build();
                
                log.info("새 Stage6 사업계획서 생성");
            }
            
            // 4. DB 저장
            Stage6BizplanSummary saved = stage6BizplanSummaryRepository.save(bizplanSummary);
            
            log.info("Stage6 사업계획서 저장 완료 - stage6Code: {}, 텍스트 길이: {}자", 
                     saved.getStage6Code(), parsedContent.length());
            
            return Stage6BizPlanParseRespDto.success(parsedContent);
            
        } catch (Exception e) {
            log.error("Stage6 사업계획서 업로드 실패", e);
            throw new RuntimeException("사업계획서 업로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 2단계: 글로벌 사업계획서 생성 및 저장
     * @param eventCode 행사코드
     * @param teamCode 팀코드
     * @return 글로벌화된 사업계획서 내용
     */
    @Transactional
    public String generateGlobalBizPlan(Integer eventCode, Integer teamCode) {
        log.info("글로벌 사업계획서 생성 시작 - eventCode: {}, teamCode: {}", eventCode, teamCode);
        
        try {
            // 1. 기존 데이터 조회
            Stage6BizplanSummary existing = stage6BizplanSummaryRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("사업계획서를 먼저 업로드해주세요."));
            
            if (existing.getBizItemSummary() == null || existing.getBizItemSummary().trim().isEmpty()) {
                throw new RuntimeException("사업계획서 내용이 없습니다. 파일을 다시 업로드해주세요.");
            }
            
            // 2. ChatGPT로 글로벌화 첨삭
            String globalizedContent = globalizeWithChatGPT(existing.getBizItemSummary());
            
            // 3. 워드 파일 생성 및 Firebase 업로드 (임시)
            String globalBizplanFilePath = uploadGlobalBizplanToFirebase(globalizedContent, eventCode, teamCode);
            
            // 4. DB 업데이트
            existing.setGlobalBizplanFilePath(globalBizplanFilePath);
            existing.setGlobalBizItemSummary(globalizedContent);
            stage6BizplanSummaryRepository.save(existing);
            
            log.info("글로벌 사업계획서 생성 완료 - stage6Code: {}, 글로벌 텍스트 길이: {}자", 
                     existing.getStage6Code(), globalizedContent.length());
            
            return globalizedContent;
            
        } catch (Exception e) {
            log.error("글로벌 사업계획서 생성 실패", e);
            throw new RuntimeException("글로벌 사업계획서 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * ChatGPT로 사업계획서 글로벌화
     */
    private String globalizeWithChatGPT(String koreanBizPlan) {
        try {
            log.info("ChatGPT 글로벌화 요청 시작 - 원본 길이: {}자", koreanBizPlan.length());
            
            String prompt = createGlobalizationPrompt(koreanBizPlan);
            
            // ChatGPT API 호출
            String response = businessPlanAnalyzer.callChatGptApi(prompt);
            
            // 응답에서 순수 텍스트 추출
            String globalizedContent = extractGlobalizedContent(response);
            
            log.info("ChatGPT 글로벌화 완료 - 글로벌 길이: {}자", globalizedContent.length());
            return globalizedContent;
            
        } catch (Exception e) {
            log.error("ChatGPT 글로벌화 실패", e);
            throw new RuntimeException("ChatGPT 글로벌화 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 글로벌화 프롬프트 생성
     */
    private String createGlobalizationPrompt(String koreanBizPlan) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 글로벌 사업계획서 전문가입니다.\n\n");
        prompt.append("# 글로벌 사업계획서 첨삭 요청\n\n");
        prompt.append("아래는 한국어로 작성된 사업계획서입니다. 이를 해외진출을 목표로 하는 글로벌 사업계획서로 첨삭해주세요.\n\n");
        
        prompt.append("## 원본 한국어 사업계획서\n");
        prompt.append(koreanBizPlan).append("\n\n");
        
        prompt.append("## 글로벌화 요구사항\n");
        prompt.append("1. **해외 시장 진출 관점**: 미국, 중국, 일본 등 주요 해외 시장 진출을 고려한 내용으로 수정\n");
        prompt.append("2. **글로벌 시장 분석**: 해외 시장 규모, 경쟁 환경, 진출 전략 추가\n");
        prompt.append("3. **국제 경쟁력**: 글로벌 경쟁사 대비 차별화 요소 강조\n");
        prompt.append("4. **현지화 전략**: 각 국가별 맞춤 전략 및 현지화 방안\n");
        prompt.append("5. **글로벌 파트너십**: 해외 파트너, 유통업체, 투자자 확보 방안\n");
        prompt.append("6. **국제 규제 및 인증**: 해외 진출에 필요한 인증, 규제 대응 방안\n");
        prompt.append("7. **글로벌 마케팅**: 해외 고객 타겟팅 및 마케팅 전략\n");
        prompt.append("8. **언어**: 한국어로 작성하되, 글로벌한 관점과 용어 사용\n\n");
        
        prompt.append("## 주의사항\n");
        prompt.append("- 기존 사업 아이템의 핵심은 유지하면서 글로벌 관점 추가\n");
        prompt.append("- 구체적인 해외 시장 데이터와 전략 포함\n");
        prompt.append("- 실현 가능한 글로벌 진출 계획 수립\n");
        prompt.append("- 한국어로 작성하되 국제적 감각 반영\n\n");
        
        prompt.append("**응답 형식**: 글로벌화된 사업계획서 내용만 출력해주세요. 추가 설명이나 메타 코멘트는 제외하고 순수한 사업계획서 내용만 작성해주세요.");
        
        return prompt.toString();
    }
    
    /**
     * ChatGPT 응답에서 글로벌화된 내용 추출
     */
    private String extractGlobalizedContent(String response) {
        // 간단한 정제: 메타 코멘트 제거
        String cleaned = response.trim();
        
        // ChatGPT가 종종 추가하는 메타 설명 제거
        if (cleaned.startsWith("글로벌화된 사업계획서:") || cleaned.startsWith("다음은 글로벌화된")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1).trim();
            }
        }
        
        // 텍스트 길이 제한
        if (cleaned.length() > MAX_TEXT_LENGTH) {
            cleaned = limitTextLength(cleaned, MAX_TEXT_LENGTH);
            log.warn("글로벌화된 텍스트가 {}자에서 {}자로 축소되었습니다.", cleaned.length(), MAX_TEXT_LENGTH);
        }
        
        return cleaned;
    }
    
    /**
     * 글로벌 사업계획서 워드 파일 생성 및 Firebase 업로드 (임시)
     */
    private String uploadGlobalBizplanToFirebase(String content, Integer eventCode, Integer teamCode) {
        try {
            // TODO: 실제 워드 파일 생성 및 Firebase 업로드 구현
            // 현재는 임시 URL 반환
            
            /*
            // 실제 구현 예시 (주석 처리)
            // 1. 워드 파일 생성
            byte[] wordFileBytes = createWordDocument(content);
            
            // 2. Firebase 업로드
            String fileName = String.format("global_bizplan_%d_%d_%d.docx", 
                                          eventCode, teamCode, System.currentTimeMillis());
            String firebaseUrl = firebaseStorageService.uploadFile(wordFileBytes, fileName);
            
            log.info("글로벌 사업계획서 Firebase 업로드 완료 - URL: {}", firebaseUrl);
            return firebaseUrl;
            */
            
            // 임시 URL (테스트용)
            String tempUrl = String.format("https://firebasestorage.googleapis.com/global_bizplan_%d_%d_%d.docx", 
                                         eventCode, teamCode, System.currentTimeMillis());
            
            log.info("글로벌 사업계획서 임시 URL 생성 - URL: {}", tempUrl);
            return tempUrl;
            
        } catch (Exception e) {
            log.error("글로벌 사업계획서 Firebase 업로드 실패", e);
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 행사 및 팀 유효성 검증
     */
    private void validateEventAndTeam(Integer eventCode, Integer teamCode) {
        // 행사 존재 확인
        Optional<Event> optionalEvent = eventRepository.findById(eventCode);
        if (optionalEvent.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 행사입니다.");
        }
        
        // 팀 존재 확인
        Optional<TeamMst> optionalTeam = teamMstRepository.findById(teamCode);
        if (optionalTeam.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 팀입니다.");
        }
        
        // 팀이 해당 행사에 속하는지 확인
        TeamMst team = optionalTeam.get();
        if (!team.getEventCode().equals(eventCode)) {
            throw new IllegalArgumentException("팀이 해당 행사에 속하지 않습니다.");
        }
    }
    
    /**
     * 파일에서 텍스트 추출 및 길이 제한
     */
    private String extractAndLimitText(MultipartFile file) {
        try {
            // 파일 형식 검증
            String filename = file.getOriginalFilename();
            if (!fileProcessingService.isValidFileFormat(filename)) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. PDF 또는 DOCX 파일을 업로드해주세요.");
            }
            
            // 텍스트 추출 (IOException 처리)
            String extractedText = fileProcessingService.extractTextFromFile(file);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new RuntimeException("파일에서 텍스트를 추출할 수 없습니다. 파일이 손상되었거나 텍스트가 없는 파일입니다.");
            }
            
            // 길이 제한
            if (extractedText.length() > MAX_TEXT_LENGTH) {
                extractedText = limitTextLength(extractedText, MAX_TEXT_LENGTH);
                log.warn("텍스트가 {}자에서 {}자로 축소되었습니다.", extractedText.length(), MAX_TEXT_LENGTH);
            }
            
            return extractedText;
            
        } catch (IOException e) {
            log.error("파일 처리 중 IO 오류 발생", e);
            throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e; // 파일 형식 오류는 그대로 전달
        } catch (Exception e) {
            log.error("파일 텍스트 추출 중 예상치 못한 오류", e);
            throw new RuntimeException("파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 사업계획서 파일 파싱 (기존 메서드 - 호환성 유지)
     */
    public Stage6BizPlanParseRespDto parseBizPlanFile(MultipartFile file) {
        log.info("Stage6 사업계획서 파싱 시작 - fileName: {}, size: {} bytes", 
                 file.getOriginalFilename(), file.getSize());
        
        try {
            String parsedContent = extractAndLimitText(file);
            log.info("Stage6 사업계획서 파싱 완료 - 추출된 텍스트 길이: {}자", parsedContent.length());
            return Stage6BizPlanParseRespDto.success(parsedContent);
            
        } catch (IllegalArgumentException e) {
            log.warn("파일 형식 오류: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("사업계획서 파싱 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("사업계획서 파싱 중 예상치 못한 오류", e);
            throw new RuntimeException("파일 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 3단계: 미중일 국가별 사업계획서 생성
     * @param eventCode 행사코드
     * @param teamCode 팀코드
     * @return 국가별 생성 결과
     */
    @Transactional
    public Stage6CountryBizPlanRespDto generateCountryBizPlans(Integer eventCode, Integer teamCode) {
        log.info("미중일 국가별 사업계획서 생성 시작 - eventCode: {}, teamCode: {}", eventCode, teamCode);
        
        try {
            // 1. 기존 데이터 조회
            Stage6BizplanSummary existing = stage6BizplanSummaryRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("기존 데이터가 없습니다. 먼저 글로벌 사업계획서를 생성해주세요."));
            
            if (existing.getGlobalBizItemSummary() == null || existing.getGlobalBizItemSummary().trim().isEmpty()) {
                throw new RuntimeException("글로벌 사업계획서 내용이 없습니다. 먼저 글로벌 사업계획서를 생성해주세요.");
            }
            
            // 2. 미중일 순서로 생성
            CountryGenerationResult usaResult = generateCountryBizPlan("USA", existing);
            CountryGenerationResult chinaResult = generateCountryBizPlan("CHINA", existing);
            CountryGenerationResult japanResult = generateCountryBizPlan("JAPAN", existing);
            
            // 3. DB 저장 (각국별로 성공한 경우만)
            boolean updated = false;
            
            if (usaResult.isSuccess()) {
                existing.setUsaSummary(usaResult.getMessage());
                updated = true;
                log.info("미국 사업계획서 DB 저장 완료");
            }
            
            if (chinaResult.isSuccess()) {
                existing.setChinaSummary(chinaResult.getMessage());
                updated = true;
                log.info("중국 사업계획서 DB 저장 완료");
            }
            
            if (japanResult.isSuccess()) {
                existing.setJapanSummary(japanResult.getMessage());
                updated = true;
                log.info("일본 사업계획서 DB 저장 완료");
            }
            
            // DB 업데이트 (하나라도 성공했으면)
            if (updated) {
                stage6BizplanSummaryRepository.save(existing);
                log.info("국가별 사업계획서 DB 업데이트 완료");
            }
            
            log.info("미중일 국가별 사업계획서 생성 완료 - USA: {}, CHINA: {}, JAPAN: {}", 
                     usaResult.isSuccess(), chinaResult.isSuccess(), japanResult.isSuccess());
            
            return Stage6CountryBizPlanRespDto.create(usaResult, chinaResult, japanResult);
            
        } catch (Exception e) {
            log.error("미중일 국가별 사업계획서 생성 실패", e);
            throw new RuntimeException("국가별 사업계획서 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 개별 국가 사업계획서 생성 (3번 재시도)
     */
    private CountryGenerationResult generateCountryBizPlan(String country, Stage6BizplanSummary existing) {
        String countryName = getCountryName(country);
        log.info("{} 사업계획서 생성 시작", countryName);
        
        // 3번 재시도
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                log.info("{} 사업계획서 생성 시도 {}/3", countryName, attempt);
                
                // GlobalBusinessPlanService 호출
                String countryBizPlan = globalBusinessPlanService.generateGlobalBusinessPlan(
                    country, 
                    existing.getGlobalBizItemSummary(), 
                    null // stageAnswers는 null 처리
                );
                
                if (countryBizPlan == null || countryBizPlan.trim().isEmpty()) {
                    throw new RuntimeException("생성된 사업계획서가 비어있습니다.");
                }
                
                // 텍스트 길이 제한
                if (countryBizPlan.length() > MAX_TEXT_LENGTH) {
                    countryBizPlan = limitTextLength(countryBizPlan, MAX_TEXT_LENGTH);
                    log.warn("{} 사업계획서가 {}자에서 {}자로 축소되었습니다.", 
                             countryName, countryBizPlan.length(), MAX_TEXT_LENGTH);
                }
                
                log.info("{} 사업계획서 생성 성공 - 길이: {}자", countryName, countryBizPlan.length());
                return CountryGenerationResult.success(countryBizPlan);
                
            } catch (Exception e) {
                log.warn("{} 사업계획서 생성 실패 (시도 {}/3): {}", countryName, attempt, e.getMessage());
                
                if (attempt == 3) {
                    // 3번 모두 실패
                    String errorMessage = String.format("%s 사업계획서 생성 실패 (3번 시도 후 포기): %s", 
                                                       countryName, e.getMessage());
                    log.error(errorMessage);
                    return CountryGenerationResult.failure(errorMessage);
                }
                
                // 재시도 전 잠시 대기 (1초)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // 이 라인에 도달할 수 없지만 안전을 위해
        return CountryGenerationResult.failure(countryName + " 사업계획서 생성 실패");
    }
    
    /**
     * 국가 코드를 한국어 이름으로 변환
     */
    private String getCountryName(String country) {
        switch (country) {
            case "USA": return "미국";
            case "CHINA": return "중국";
            case "JAPAN": return "일본";
            default: return country;
        }
    }
    
    /**
     * 미국 사업계획서 조회
     */
    public Stage6CountryBizPlanViewRespDto getUsaBizPlan(Integer eventCode, Integer teamCode) {
        try {
            log.info("미국 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6BizplanSummary bizplanSummary = stage6BizplanSummaryRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("사업계획서를 찾을 수 없습니다."));
            
            String usaSummary = bizplanSummary.getUsaSummary();
            if (usaSummary == null || usaSummary.trim().isEmpty()) {
                throw new RuntimeException("미국 사업계획서가 아직 생성되지 않았습니다.");
            }
            
            log.info("미국 사업계획서 조회 성공 - 길이: {}자", usaSummary.length());
            return Stage6CountryBizPlanViewRespDto.success(usaSummary);
            
        } catch (Exception e) {
            log.error("미국 사업계획서 조회 실패", e);
            throw new RuntimeException("미국 사업계획서 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 중국 사업계획서 조회
     */
    public Stage6CountryBizPlanViewRespDto getChinaBizPlan(Integer eventCode, Integer teamCode) {
        try {
            log.info("중국 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6BizplanSummary bizplanSummary = stage6BizplanSummaryRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("사업계획서를 찾을 수 없습니다."));
            
            String chinaSummary = bizplanSummary.getChinaSummary();
            if (chinaSummary == null || chinaSummary.trim().isEmpty()) {
                throw new RuntimeException("중국 사업계획서가 아직 생성되지 않았습니다.");
            }
            
            log.info("중국 사업계획서 조회 성공 - 길이: {}자", chinaSummary.length());
            return Stage6CountryBizPlanViewRespDto.success(chinaSummary);
            
        } catch (Exception e) {
            log.error("중국 사업계획서 조회 실패", e);
            throw new RuntimeException("중국 사업계획서 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 일본 사업계획서 조회
     */
    public Stage6CountryBizPlanViewRespDto getJapanBizPlan(Integer eventCode, Integer teamCode) {
        try {
            log.info("일본 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6BizplanSummary bizplanSummary = stage6BizplanSummaryRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode)
                .orElseThrow(() -> new RuntimeException("사업계획서를 찾을 수 없습니다."));
            
            String japanSummary = bizplanSummary.getJapanSummary();
            if (japanSummary == null || japanSummary.trim().isEmpty()) {
                throw new RuntimeException("일본 사업계획서가 아직 생성되지 않았습니다.");
            }
            
            log.info("일본 사업계획서 조회 성공 - 길이: {}자", japanSummary.length());
            return Stage6CountryBizPlanViewRespDto.success(japanSummary);
            
        } catch (Exception e) {
            log.error("일본 사업계획서 조회 실패", e);
            throw new RuntimeException("일본 사업계획서 조회 실패: " + e.getMessage());
        }
    }
    private String limitTextLength(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        // 문장 단위로 자르기 (단어 중간에서 자르지 않도록)
        String truncated = text.substring(0, maxLength);
        
        // 마지막 완전한 문장을 찾기
        int lastPeriod = truncated.lastIndexOf('.');
        int lastExclamation = truncated.lastIndexOf('!');
        int lastQuestion = truncated.lastIndexOf('?');
        int lastNewline = truncated.lastIndexOf('\n');
        
        int cutPoint = Math.max(Math.max(lastPeriod, lastExclamation), 
                               Math.max(lastQuestion, lastNewline));
        
        if (cutPoint > maxLength - 1000) { // 너무 많이 잘리지 않도록
            return truncated.substring(0, cutPoint + 1);
        } else {
            return truncated + "...";
        }
    }
}
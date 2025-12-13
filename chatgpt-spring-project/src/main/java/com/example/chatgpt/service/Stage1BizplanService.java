package com.example.chatgpt.service;

import com.example.chatgpt.dto.stage1bizplan.respDto.Stage1BizplanViewDto;
import com.example.chatgpt.entity.Event;
import com.example.chatgpt.entity.Stage1Bizplan;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.repository.EventRepository;
import com.example.chatgpt.repository.Stage1BizplanRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class Stage1BizplanService {
    
    private final Stage1BizplanRepository stage1BizplanRepository;
    private final EventRepository eventRepository;
    private final TeamMstRepository teamMstRepository;
    private final FileProcessingService fileProcessingService;
    
    private static final int MAX_TEXT_LENGTH = 50000; // 50,000자 제한
    
    /**
     * Stage1 사업계획서 조회
     */
    public Stage1BizplanViewDto getStage1Bizplan(Integer eventCode, Integer teamCode) {
        try {
            log.info("Stage1 사업계획서 조회 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // Stage1 사업계획서 조회
            Optional<Stage1Bizplan> optionalBizplan = stage1BizplanRepository
                .findByEventCodeAndTeamCode(eventCode, teamCode);
            
            if (optionalBizplan.isEmpty()) {
                log.info("Stage1 사업계획서가 없음 - eventCode: {}, teamCode: {}", eventCode, teamCode);
                return null;
            }
            
            Stage1Bizplan bizplan = optionalBizplan.get();
            
            // DTO 변환
            Stage1BizplanViewDto result = Stage1BizplanViewDto.from(bizplan);
            
            log.info("Stage1 사업계획서 조회 완료 - stage1Code: {}, 파일 경로: {}, 내용 길이: {}자", 
                     bizplan.getStage1Code(), 
                     bizplan.getBizplanFilePath(),
                     bizplan.getBizplanContent() != null ? bizplan.getBizplanContent().length() : 0);
            
            return result;
            
        } catch (Exception e) {
            log.error("Stage1 사업계획서 조회 실패", e);
            throw new RuntimeException("Stage1 사업계획서 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 사업계획서 업로드 및 DB 저장
     * @param eventCode 행사코드
     * @param teamCode 팀코드
     * @param file 업로드된 파일 (텍스트 추출용)
     * @param fileUrl Firebase Storage URL (저장용)
     */
    @Transactional
    public Stage1Bizplan uploadBizplan(Integer eventCode, Integer teamCode, MultipartFile file, String fileUrl) {
        log.info("사업계획서 업로드 시작 - eventCode: {}, teamCode: {}, fileName: {}", 
                 eventCode, teamCode, file.getOriginalFilename());
        
        try {
            // 1. 행사 존재 여부 확인
            Optional<Event> optionalEvent = eventRepository.findById(eventCode);
            if (optionalEvent.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 행사입니다.");
            }
            
            // 2. 팀 존재 여부 확인
            Optional<TeamMst> optionalTeam = teamMstRepository.findById(teamCode);
            if (optionalTeam.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 팀입니다.");
            }
            
            // 3. 팀이 해당 행사에 속하는지 확인
            TeamMst team = optionalTeam.get();
            if (!team.getEventCode().equals(eventCode)) {
                throw new IllegalArgumentException("팀이 해당 행사에 속하지 않습니다.");
            }
            
            // 4. 파일 형식 검증
            String filename = file.getOriginalFilename();
            if (!fileProcessingService.isValidFileFormat(filename)) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. PDF 또는 DOCX 파일을 업로드해주세요.");
            }
            
            // 5. ✅ 파일에서 텍스트 추출
            String extractedText = fileProcessingService.extractTextFromFile(file);
            
            // 6. 텍스트 길이 제한
            String limitedText = extractedText;
            if (extractedText.length() > MAX_TEXT_LENGTH) {
                limitedText = limitTextLength(extractedText, MAX_TEXT_LENGTH);
                log.warn("텍스트가 {}자에서 {}자로 축소되었습니다.", extractedText.length(), MAX_TEXT_LENGTH);
            }
            
            // 7. 기존 사업계획서 확인 (덮어쓰기)
            Optional<Stage1Bizplan> existingBizplan = stage1BizplanRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            
            Stage1Bizplan bizplan;
            
            if (existingBizplan.isPresent()) {
                // 덮어쓰기
                bizplan = existingBizplan.get();
                bizplan.setBizplanFilePath(fileUrl);      // Firebase URL
                bizplan.setBizplanContent(limitedText);   // 추출된 텍스트
                bizplan.setBizItemSummary(null);          // 추후 업데이트용
                
                log.info("기존 사업계획서 덮어쓰기 - stage1Code: {}", bizplan.getStage1Code());
                
            } else {
                // 새로 생성
                bizplan = Stage1Bizplan.builder()
                    .eventCode(eventCode)
                    .teamCode(teamCode)
                    .bizplanFilePath(fileUrl)      // Firebase URL
                    .bizplanContent(limitedText)   // 추출된 텍스트
                    .bizItemSummary(null)          // 추후 업데이트용
                    .build();
                
                log.info("새 사업계획서 생성");
            }
            
            // 8. DB 저장
            Stage1Bizplan savedBizplan = stage1BizplanRepository.save(bizplan);
            
            log.info("사업계획서 업로드 완료 - stage1Code: {}, 텍스트 길이: {}자, Firebase URL: {}", 
                     savedBizplan.getStage1Code(), savedBizplan.getBizplanContent().length(), fileUrl);
            
            return savedBizplan;
            
        } catch (Exception e) {
            log.error("사업계획서 업로드 실패", e);
            throw new RuntimeException("사업계획서 업로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 텍스트 길이 제한
     */
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
    
    /**
     * 사업계획서 조회 (getBizplan → getBizplan으로 수정)
     */
    public Stage1Bizplan getBizplan(Integer eventCode, Integer teamCode) {
        try {
            return stage1BizplanRepository.findByEventCodeAndTeamCode(eventCode, teamCode).orElse(null);
        } catch (Exception e) {
            log.error("사업계획서 조회 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            return null;
        }
    }
    
    /**
     * 사업계획서 존재 여부 확인
     */
    public boolean existsBizplan(Integer eventCode, Integer teamCode) {
        try {
            return stage1BizplanRepository.existsByEventCodeAndTeamCode(eventCode, teamCode);
        } catch (Exception e) {
            log.error("사업계획서 존재 여부 확인 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            return false;
        }
    }
}
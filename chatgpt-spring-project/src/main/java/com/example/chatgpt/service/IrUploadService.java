package com.example.chatgpt.service;

import com.example.chatgpt.entity.Event;
import com.example.chatgpt.entity.IrUpload;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.repository.EventRepository;
import com.example.chatgpt.repository.IrUploadRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class IrUploadService {
    
    private final IrUploadRepository irUploadRepository;
    private final EventRepository eventRepository;
    private final TeamMstRepository teamMstRepository;
    private final FileProcessingService fileProcessingService;
    
    private static final int MAX_TEXT_LENGTH = 50000; // 50,000자 제한 (TEXT 컬럼 고려)
    
    /**
     * IR 자료 업로드 및 DB 저장
     * @param eventCode 행사코드
     * @param teamCode 팀코드
     * @param irFile IR 파일 (선택적)
     * @param wordFile 워드 파일 (텍스트 추출용)
     * @param irFileUrl IR 파일 Firebase URL
     * @param wordFileUrl 워드 파일 Firebase URL
     */
    @Transactional
    public IrUpload uploadIrFiles(Integer eventCode, Integer teamCode, 
                                 MultipartFile irFile, MultipartFile wordFile,
                                 String irFileUrl, String wordFileUrl) {
        log.info("IR 자료 업로드 시작 - eventCode: {}, teamCode: {}", eventCode, teamCode);
        
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
            
            // 4. 워드 파일에서 텍스트 추출 (있는 경우만)
            String extractedText = null;
            if (wordFile != null && !wordFile.isEmpty()) {
                // 파일 형식 검증
                String filename = wordFile.getOriginalFilename();
                if (!fileProcessingService.isValidFileFormat(filename)) {
                    throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. PDF 또는 DOCX 파일을 업로드해주세요.");
                }
                
                // 텍스트 추출
                extractedText = fileProcessingService.extractTextFromFile(wordFile);
                
                // 텍스트 길이 제한
                if (extractedText.length() > MAX_TEXT_LENGTH) {
                    extractedText = limitTextLength(extractedText, MAX_TEXT_LENGTH);
                    log.warn("워드 텍스트가 {}자에서 {}자로 축소되었습니다.", 
                             extractedText.length(), MAX_TEXT_LENGTH);
                }
            }
            
            // 5. 기존 IR 자료 확인 (덮어쓰기)
            Optional<IrUpload> existingIr = irUploadRepository.findByEventCodeAndTeamCode(eventCode, teamCode);
            
            IrUpload irUpload;
            
            if (existingIr.isPresent()) {
                // 덮어쓰기
                irUpload = existingIr.get();
                irUpload.setIrFilePath(irFileUrl);              // IR 파일 Firebase URL
                irUpload.setIrWordFilePath(wordFileUrl);        // 워드 파일 Firebase URL
                irUpload.setIrWordContents(extractedText);      // 추출된 텍스트
                
                log.info("기존 IR 자료 덮어쓰기 - irCode: {}", irUpload.getIrCode());
                
            } else {
                // 새로 생성
                irUpload = IrUpload.builder()
                    .eventCode(eventCode)
                    .teamCode(teamCode)
                    .irFilePath(irFileUrl)              // IR 파일 Firebase URL
                    .irWordFilePath(wordFileUrl)        // 워드 파일 Firebase URL
                    .irWordContents(extractedText)      // 추출된 텍스트
                    .build();
                
                log.info("새 IR 자료 생성");
            }
            
            // 6. DB 저장
            IrUpload savedIr = irUploadRepository.save(irUpload);
            
            log.info("IR 자료 업로드 완료 - irCode: {}, 워드 텍스트 길이: {}자", 
                     savedIr.getIrCode(), 
                     savedIr.getIrWordContents() != null ? savedIr.getIrWordContents().length() : 0);
            
            return savedIr;
            
        } catch (Exception e) {
            log.error("IR 자료 업로드 실패", e);
            throw new RuntimeException("IR 자료 업로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 텍스트 길이 제한 (Stage1BizplanService와 동일)
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
     * IR 자료 조회
     */
    public IrUpload getIrUpload(Integer eventCode, Integer teamCode) {
        try {
            return irUploadRepository.findByEventCodeAndTeamCode(eventCode, teamCode).orElse(null);
        } catch (Exception e) {
            log.error("IR 자료 조회 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            return null;
        }
    }
    
    /**
     * IR 자료 존재 여부 확인
     */
    public boolean existsIrUpload(Integer eventCode, Integer teamCode) {
        try {
            return irUploadRepository.existsByEventCodeAndTeamCode(eventCode, teamCode);
        } catch (Exception e) {
            log.error("IR 자료 존재 여부 확인 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            return false;
        }
    }
}
package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.stage1bizplan.respDto.Stage1BizplanViewDto;
import com.example.chatgpt.entity.Stage1Bizplan;
import com.example.chatgpt.service.Stage1BizplanService;
import com.example.chatgpt.service.EventService;
import com.example.chatgpt.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class Stage1BizplanController {
    
    private final Stage1BizplanService stage1BizplanService;
    private final EventService eventService;
    private final TeamService teamService;
    
    /**
     * Stage1 사업계획서 조회 API
     * GET /api/v1/stage1/bizplan/list?eventCode=1&teamCode=5
     */
    @GetMapping("/stage1/bizplan/list")
    public ResponseEntity<RespDto<Stage1BizplanViewDto>> getStage1Bizplan(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("Stage1 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage1BizplanViewDto result = stage1BizplanService.getStage1Bizplan(eventCode, teamCode);
            
            if (result == null) {
                return ResponseEntity.ok(RespDto.success("Stage1 사업계획서가 없습니다.", null));
            } else {
                return ResponseEntity.ok(RespDto.success("Stage1 사업계획서 조회 성공", result));
            }
            
        } catch (RuntimeException e) {
            log.warn("Stage1 사업계획서 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Stage1 사업계획서 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("Stage1 사업계획서 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 사업계획서 업로드 API
     * POST /api/v1/stage1/bizplan/upload
     */
    @PostMapping("/stage1/bizplan/upload")
    public RespDto<Boolean> uploadBizplan(
            @RequestParam("file") MultipartFile file,           // 파일 (텍스트 추출용)
            @RequestParam("eventCode") Integer eventCode,       // 행사코드
            @RequestParam("teamCode") Integer teamCode,         // 팀코드
            @RequestParam("fileUrl") String fileUrl) {          // Firebase URL (저장용)
        
        try {
            log.info("사업계획서 업로드 요청 - eventCode: {}, teamCode: {}, fileName: {}, fileUrl: {}", 
                     eventCode, teamCode, file.getOriginalFilename(), fileUrl);
            
            // 파일 검증
            if (file.isEmpty()) {
                return RespDto.fail("파일이 비어있습니다.");
            }
            
            // 사업계획서 업로드 및 저장 (파일 + Firebase URL)
            Stage1Bizplan savedBizplan = stage1BizplanService.uploadBizplan(eventCode, teamCode, file, fileUrl);
            
            log.info("사업계획서 업로드 성공 - stage1Code: {}, 텍스트 길이: {}자", 
                     savedBizplan.getStage1Code(), 
                     savedBizplan.getBizplanContent() != null ? savedBizplan.getBizplanContent().length() : 0);
            
            return RespDto.success("사업계획서가 성공적으로 업로드되었습니다.", true);
            
        } catch (IllegalArgumentException e) {
            log.warn("사업계획서 업로드 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("사업계획서 업로드 실패", e);
            return RespDto.fail("사업계획서 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 사업아이템요약 조회 API
     * GET /api/v1/stage1/biz-item/{eventCode}/{teamCode}
     */
    @GetMapping("/stage1/biz-item/{eventCode}/{teamCode}")
    public RespDto<String> getBizItemSummary(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode) {
        
        try {
            log.info("사업아이템요약 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            // 1. 행사코드 검증
            if (!eventService.existsByEventCode(eventCode)) {
                return RespDto.fail("존재하지 않는 행사입니다.");
            }
            
            // 2. 팀코드 검증
            if (!teamService.existsByTeamCode(teamCode)) {
                return RespDto.fail("존재하지 않는 팀입니다.");
            }
            
            // 3. 사업계획서 조회
            Stage1Bizplan bizplan = stage1BizplanService.getBizplan(eventCode, teamCode);
            
            if (bizplan == null) {
                return RespDto.fail("사업계획서를 찾을 수 없습니다.");
            }
            
            // 4. bizItemSummary 반환 (빈 문자열이어도 성공으로 처리)
            String bizItemSummary = bizplan.getBizItemSummary() != null ? bizplan.getBizItemSummary() : "";
            
            log.info("사업아이템요약 조회 성공 - eventCode: {}, teamCode: {}, 요약 길이: {}자", 
                     eventCode, teamCode, bizItemSummary.length());
            
            return RespDto.success("사업아이템요약 조회 성공", bizItemSummary);
            
        } catch (Exception e) {
            log.error("사업아이템요약 조회 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            return RespDto.fail("사업아이템요약 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
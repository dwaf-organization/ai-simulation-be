package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.stage6bizplan.reqDto.Stage6BizPlanGlobalizeReqDto;
import com.example.chatgpt.dto.stage6bizplan.reqDto.Stage6CountryBizPlanReqDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6BizPlanParseRespDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6BizplanDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6BizplanGlobalizeDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6BizplanGlobalizeListRespDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6BizplanListRespDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6CountryBizPlanRespDto;
import com.example.chatgpt.dto.stage6bizplan.respDto.Stage6CountryBizPlanViewRespDto;
import com.example.chatgpt.service.Stage6BizPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class Stage6BizPlanController {
    
    private final Stage6BizPlanService stage6BizPlanService;
    
    /**
     * Stage6 사업계획서 목록 조회 API
     * GET /api/v1/stage6/bizplan/list?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/stage6/bizplan/list")
    public ResponseEntity<RespDto<List<Stage6BizplanDto>>> getBizplanList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("Stage6 사업계획서 목록 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6BizplanListRespDto result = stage6BizPlanService.getBizplanList(eventCode, teamCode);
            
            if (result.getBizplans().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("Stage6 사업계획서가 없습니다.", result.getBizplans()));
            } else {
                return ResponseEntity.ok(RespDto.success("Stage6 사업계획서 조회 성공", result.getBizplans()));
            }
            
        } catch (RuntimeException e) {
            log.warn("Stage6 사업계획서 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Stage6 사업계획서 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("Stage6 사업계획서 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * Stage6 글로벌 사업계획서 목록 조회 API
     * GET /api/v1/stage6/bizplan/globalize/list?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/stage6/bizplan/globalize/list")
    public ResponseEntity<RespDto<List<Stage6BizplanGlobalizeDto>>> getGlobalizeBizplanList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("Stage6 글로벌 사업계획서 목록 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6BizplanGlobalizeListRespDto result = stage6BizPlanService.getGlobalizeBizplanList(eventCode, teamCode);
            
            if (result.getGlobalBizplans().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("Stage6 글로벌 사업계획서가 없습니다.", result.getGlobalBizplans()));
            } else {
                return ResponseEntity.ok(RespDto.success("Stage6 글로벌 사업계획서 조회 성공", result.getGlobalBizplans()));
            }
            
        } catch (RuntimeException e) {
            log.warn("Stage6 글로벌 사업계획서 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Stage6 글로벌 사업계획서 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("Stage6 글로벌 사업계획서 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 1단계: Stage6 사업계획서 업로드 및 DB 저장 API
     * POST /api/v1/stage6/bizplan/upload
     */
    @PostMapping("/api/v1/stage6/bizplan/upload")
    public ResponseEntity<RespDto<Stage6BizPlanParseRespDto>> uploadBizPlan(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizplanFilePath") String bizplanFilePath) {
        
        try {
            log.info("Stage6 사업계획서 업로드 요청 - eventCode: {}, teamCode: {}, fileName: {}", 
                     eventCode, teamCode, file.getOriginalFilename());
            
            // 파일 업로드 체크
            if (file.isEmpty()) {
                return ResponseEntity.ok(RespDto.fail("업로드된 파일이 없습니다."));
            }
            
            // 사업계획서 업로드 및 DB 저장
            Stage6BizPlanParseRespDto result = stage6BizPlanService
                .uploadAndParseBizPlan(eventCode, teamCode, file, bizplanFilePath);
            
            return ResponseEntity.ok(RespDto.success("사업계획서 업로드 및 파싱 완료", result));
            
        } catch (IllegalArgumentException e) {
            log.warn("업로드 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (RuntimeException e) {
            log.warn("업로드 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Stage6 사업계획서 업로드 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("파일 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 2단계: 글로벌 사업계획서 생성 API
     * POST /api/v1/stage6/bizplan/globalize
     */
    @PostMapping("/api/v1/stage6/bizplan/globalize")
    public ResponseEntity<RespDto<String>> generateGlobalBizPlan(
            @RequestBody Stage6BizPlanGlobalizeReqDto request) {
        
        try {
            log.info("글로벌 사업계획서 생성 요청 - eventCode: {}, teamCode: {}", 
                     request.getEventCode(), request.getTeamCode());
            
            // 글로벌 사업계획서 생성
            String globalizedContent = stage6BizPlanService.generateGlobalBizPlan(
                request.getEventCode(), request.getTeamCode());
            
            return ResponseEntity.ok(RespDto.success("글로벌 사업계획서 생성 완료", globalizedContent));
            
        } catch (RuntimeException e) {
            log.warn("글로벌화 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("글로벌 사업계획서 생성 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("글로벌화 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 3단계: 미중일 국가별 사업계획서 생성 API
     * POST /api/v1/stage6/bizplan/generate-countries
     */
    @PostMapping("/api/v1/stage6/bizplan/generate-countries")
    public ResponseEntity<RespDto<Stage6CountryBizPlanRespDto>> generateCountryBizPlans(
            @RequestBody Stage6CountryBizPlanReqDto request) {
        
        try {
            log.info("미중일 국가별 사업계획서 생성 요청 - eventCode: {}, teamCode: {}", 
                     request.getEventCode(), request.getTeamCode());
            
            // 미중일 국가별 사업계획서 생성
            Stage6CountryBizPlanRespDto result = stage6BizPlanService.generateCountryBizPlans(
                request.getEventCode(), request.getTeamCode());
            
            return ResponseEntity.ok(RespDto.success("미중일 국가별 사업계획서 생성 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("국가별 사업계획서 생성 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("미중일 국가별 사업계획서 생성 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("국가별 사업계획서 생성 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 미국 사업계획서 조회 API
     * GET /api/v1/stage6/bizplan/usa
     */
    @GetMapping("/api/v1/stage6/bizplan/usa")
    public ResponseEntity<RespDto<Stage6CountryBizPlanViewRespDto>> getUsaBizPlan(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("미국 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6CountryBizPlanViewRespDto result = stage6BizPlanService.getUsaBizPlan(eventCode, teamCode);
            
            return ResponseEntity.ok(RespDto.success("미국 사업계획서 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("미국 사업계획서 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("미국 사업계획서 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("미국 사업계획서 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 중국 사업계획서 조회 API
     * GET /api/v1/stage6/bizplan/china
     */
    @GetMapping("/api/v1/stage6/bizplan/china")
    public ResponseEntity<RespDto<Stage6CountryBizPlanViewRespDto>> getChinaBizPlan(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("중국 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6CountryBizPlanViewRespDto result = stage6BizPlanService.getChinaBizPlan(eventCode, teamCode);
            
            return ResponseEntity.ok(RespDto.success("중국 사업계획서 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("중국 사업계획서 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("중국 사업계획서 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("중국 사업계획서 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 일본 사업계획서 조회 API
     * GET /api/v1/stage6/bizplan/japan
     */
    @GetMapping("/api/v1/stage6/bizplan/japan")
    public ResponseEntity<RespDto<Stage6CountryBizPlanViewRespDto>> getJapanBizPlan(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("일본 사업계획서 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            Stage6CountryBizPlanViewRespDto result = stage6BizPlanService.getJapanBizPlan(eventCode, teamCode);
            
            return ResponseEntity.ok(RespDto.success("일본 사업계획서 조회 완료", result));
            
        } catch (RuntimeException e) {
            log.warn("일본 사업계획서 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("일본 사업계획서 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("일본 사업계획서 조회 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * [기존] Stage6 사업계획서 파일 파싱만 (호환성 유지)
     * POST /api/v1/stage6/bizplan/parse
     */
    @PostMapping("/api/v1/stage6/bizplan/parse")
    public ResponseEntity<RespDto<Stage6BizPlanParseRespDto>> parseBizPlanFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Stage6 사업계획서 파싱 요청 - fileName: {}, size: {} bytes", 
                     file.getOriginalFilename(), file.getSize());
            
            // 파일 업로드 체크
            if (file.isEmpty()) {
                return ResponseEntity.ok(RespDto.fail("업로드된 파일이 없습니다."));
            }
            
            // 파일 파싱만 (DB 저장 없음)
            Stage6BizPlanParseRespDto result = stage6BizPlanService.parseBizPlanFile(file);
            
            return ResponseEntity.ok(RespDto.success("사업계획서 파싱 완료", result));
            
        } catch (IllegalArgumentException e) {
            log.warn("파일 형식 오류: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (RuntimeException e) {
            log.warn("파싱 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Stage6 사업계획서 파싱 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("파일 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
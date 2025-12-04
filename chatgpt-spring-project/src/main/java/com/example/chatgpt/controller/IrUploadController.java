package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.irupload.respDto.IrUploadDto;
import com.example.chatgpt.dto.irupload.respDto.IrUploadListRespDto;
import com.example.chatgpt.entity.IrUpload;
import com.example.chatgpt.service.IrUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class IrUploadController {
    
    private final IrUploadService irUploadService;
    
    /**
     * IR 자료 업로드 API (Stage1 사업계획서 업로드와 동일한 방식)
     * POST /api/v1/ir-upload/upload
     */
    @PostMapping("/api/v1/ir-upload/upload")
    public ResponseEntity<RespDto<Integer>> uploadIrFiles(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode,
            @RequestParam(value = "irFile", required = false) MultipartFile irFile,
            @RequestParam(value = "wordFile", required = false) MultipartFile wordFile,
            @RequestParam(value = "irFileUrl", required = false) String irFileUrl,
            @RequestParam(value = "wordFileUrl", required = false) String wordFileUrl) {
        
        try {
            log.info("IR 자료 업로드 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            log.info("IR 파일: {}, 워드 파일: {}", 
                     irFile != null ? irFile.getOriginalFilename() : "없음",
                     wordFile != null ? wordFile.getOriginalFilename() : "없음");
            
            IrUpload savedIr = irUploadService.uploadIrFiles(eventCode, teamCode, 
                                                            irFile, wordFile, 
                                                            irFileUrl, wordFileUrl);
            
            return ResponseEntity.ok(RespDto.success("IR 자료 업로드 완료", savedIr.getIrCode()));
            
        } catch (IllegalArgumentException e) {
            log.warn("IR 자료 업로드 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (RuntimeException e) {
            log.warn("IR 자료 업로드 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("IR 자료 업로드 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("IR 자료 업로드 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * IR 자료 조회 API
     * GET /api/v1/ir-upload/{eventCode}/{teamCode}
     */
    @GetMapping("/api/v1/ir-upload/{eventCode}/{teamCode}")
    public ResponseEntity<RespDto<IrUpload>> getIrUpload(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode) {
        
        try {
            log.info("IR 자료 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            IrUpload irUpload = irUploadService.getIrUpload(eventCode, teamCode);
            
            if (irUpload == null) {
                return ResponseEntity.ok(RespDto.fail("IR 자료를 찾을 수 없습니다."));
            }
            
            return ResponseEntity.ok(RespDto.success("IR 자료 조회 성공", irUpload));
            
        } catch (Exception e) {
            log.error("IR 자료 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("IR 자료 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * IR 자료 존재 여부 확인 API
     * GET /api/v1/ir-upload/exists/{eventCode}/{teamCode}
     */
    @GetMapping("/api/v1/ir-upload/exists/{eventCode}/{teamCode}")
    public ResponseEntity<RespDto<Boolean>> existsIrUpload(
            @PathVariable("eventCode") Integer eventCode,
            @PathVariable("teamCode") Integer teamCode) {
        
        try {
            log.info("IR 자료 존재 여부 확인 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            boolean exists = irUploadService.existsIrUpload(eventCode, teamCode);
            
            return ResponseEntity.ok(RespDto.success("IR 자료 존재 여부 확인 완료", exists));
            
        } catch (Exception e) {
            log.error("IR 자료 존재 여부 확인 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("IR 자료 존재 여부 확인 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * IR 자료 목록 조회 API (날짜 제외)
     * GET /api/v1/ir-upload/list?eventCode=1&teamCode=5
     */
    @GetMapping("/api/v1/ir-upload/list")
    public ResponseEntity<RespDto<List<IrUploadDto>>> getIrUploadList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam("teamCode") Integer teamCode) {
        
        try {
            log.info("IR 자료 목록 조회 요청 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
            IrUploadListRespDto result = irUploadService.getIrUploadList(eventCode, teamCode);
            
            if (result.getIrUploads().isEmpty()) {
                return ResponseEntity.ok(RespDto.success("IR 자료가 없습니다.", result.getIrUploads()));
            } else {
                return ResponseEntity.ok(RespDto.success("IR 자료 조회 성공", result.getIrUploads()));
            }
            
        } catch (RuntimeException e) {
            log.warn("IR 자료 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(RespDto.fail(e.getMessage()));
            
        } catch (Exception e) {
            log.error("IR 자료 조회 중 오류 발생", e);
            return ResponseEntity.ok(RespDto.fail("IR 자료 조회 중 오류가 발생했습니다."));
        }
    }
}
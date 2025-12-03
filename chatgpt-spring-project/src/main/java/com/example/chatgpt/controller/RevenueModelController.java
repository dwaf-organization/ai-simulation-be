package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.stage1.reqDto.RevenueModelReqDto;
import com.example.chatgpt.dto.stage1.respDto.RevenueModelRespDto;
import com.example.chatgpt.entity.RevenueModel;
import com.example.chatgpt.service.RevenueModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RevenueModelController {
    
    private final RevenueModelService revenueModelService;
    
    /**
     * 수익모델 설정 API
     * POST /api/v1/stage1/revenue-model
     */
    @PostMapping("/stage1/revenue-model")
    public RespDto<RevenueModelRespDto> setRevenueModel(@Valid @RequestBody RevenueModelReqDto request) {
        
        try {
            log.info("수익모델 설정 요청 - teamCode: {}, revenueCategory: {}", 
                     request.getTeamCode(), request.getRevenueCategory());
            
            // 수익모델 설정 및 저장
            RevenueModel savedModel = revenueModelService.setRevenueModel(request);
            
            // Response DTO 변환
            RevenueModelRespDto responseData = RevenueModelRespDto.from(savedModel);
            
            log.info("수익모델 설정 성공 - revenueModelCode: {}, teamCode: {}, revenueCategory: {}", 
                     savedModel.getRevenueModelCode(), savedModel.getTeamCode(), savedModel.getRevenueCategory());
            
            return RespDto.success("수익모델이 성공적으로 설정되었습니다.", responseData);
            
        } catch (IllegalArgumentException e) {
            log.warn("수익모델 설정 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("수익모델 설정 실패", e);
            return RespDto.fail("수익모델 설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
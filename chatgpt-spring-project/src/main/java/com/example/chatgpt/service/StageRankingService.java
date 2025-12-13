package com.example.chatgpt.service;

import com.example.chatgpt.dto.stageranking.respDto.StageRankingRespDto;
import com.example.chatgpt.repository.TeamRevenueAllocationRepository;
import com.example.chatgpt.repository.FinancialStatementRepository;
import com.example.chatgpt.repository.CompanyCapabilityScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StageRankingService {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 스테이지별 팀별순위 조회
     */
    public List<StageRankingRespDto> getStageRanking(Integer eventCode, Integer stageStep) {
        log.info("스테이지별 팀별순위 조회 요청 - eventCode: {}, stageStep: {}", eventCode, stageStep);
        
        try {
            String sql = buildRankingQuery();
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, eventCode, stageStep, eventCode, stageStep, eventCode);
            
            List<StageRankingRespDto> rankings = results.stream()
                    .map(this::mapToDto)
                    .toList();
            
            log.info("스테이지별 팀별순위 조회 완료 - {}개 팀", rankings.size());
            return rankings;
            
        } catch (Exception e) {
            log.error("스테이지별 팀별순위 조회 실패 - eventCode: {}, stageStep: {}", eventCode, stageStep, e);
            throw new RuntimeException("스테이지별 팀별순위 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 4개 테이블 조인 쿼리 생성 (team_mst 추가)
     */
    private String buildRankingQuery() {
        return """
            SELECT 
                tra.team_code,
                tm.team_name,
                tra.allocated_revenue,
                tra.stage_rank,
                fs.fs_score,
                ccs.total_capability_level
            FROM team_revenue_allocation tra
            LEFT JOIN team_mst tm
                ON tra.team_code = tm.team_code
            LEFT JOIN financial_statement fs 
                ON tra.event_code = fs.event_code 
                AND tra.team_code = fs.team_code 
                AND tra.stage_step = fs.stage_step
            LEFT JOIN company_capability_score ccs 
                ON tra.event_code = ccs.event_code 
                AND tra.team_code = ccs.team_code
            WHERE tra.event_code = ? 
                AND tra.stage_step = ?
            ORDER BY tra.stage_rank ASC
            """;
    }
    
    /**
     * 쿼리 결과를 DTO로 변환
     */
    private StageRankingRespDto mapToDto(Map<String, Object> row) {
        return StageRankingRespDto.builder()
                .teamCode(convertToInteger(row.get("team_code")))
                .teamName(convertToString(row.get("team_name")))
                .allocatedRevenue(convertToInteger(row.get("allocated_revenue")))
                .stageRank(convertToInteger(row.get("stage_rank")))
                .fsScore(convertToInteger(row.get("fs_score")))
                .totalCapabilityLevel(convertToInteger(row.get("total_capability_level")))
                .build();
    }
    
    /**
     * Object를 Integer로 안전하게 변환
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    /**
     * Object를 String으로 안전하게 변환
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.PaginationDto;
import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.dto.team.reqDto.TeamCreateReqDto;
import com.example.chatgpt.dto.team.reqDto.TeamLoginReqDto;
import com.example.chatgpt.dto.team.reqDto.TeamUpdateReqDto;
import com.example.chatgpt.dto.team.respDto.TeamCreateRespDto;
import com.example.chatgpt.dto.team.respDto.TeamDeleteRespDto;
import com.example.chatgpt.dto.team.respDto.TeamListRespDto;
import com.example.chatgpt.dto.team.respDto.TeamLoginRespDto;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TeamController {
    
    private final TeamService teamService;
    
    /**
     * 팀 생성 API
     * POST /api/v1/teams/create
     */
    @PostMapping("/teams/create")
    public RespDto<TeamCreateRespDto> createTeam(@Valid @RequestBody TeamCreateReqDto request) {
        
        try {
            log.info("팀 생성 요청 - eventCode: {}", request.getEventCode());
            
            TeamMst createdTeam = teamService.createTeam(request);
            
            TeamCreateRespDto responseData = TeamCreateRespDto.from(createdTeam);
            
            log.info("팀 생성 완료 - teamCode: {}, teamId: {}", 
                     createdTeam.getTeamCode(), createdTeam.getTeamId());
            
            return RespDto.success("팀이 성공적으로 생성되었습니다.", responseData);
            
        } catch (IllegalArgumentException e) {
            log.warn("팀 생성 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("팀 생성 실패", e);
            return RespDto.fail("팀 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 팀 목록 조회 API
     * GET /api/v1/teams/list?eventCode={eventCode}&page={page}&size={size}
     */
    @GetMapping("/teams/list")
    public RespDto<TeamListRespDto> getTeamList(
            @RequestParam("eventCode") Integer eventCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            log.info("팀 목록 조회 요청 - eventCode: {}, page: {}, size: {}", eventCode, page, size);
            
            // 팀 목록 조회
            Page<TeamMst> teamPage = teamService.getTeamList(eventCode, page, size);
            
            // 행사명 조회 (한 번만)
            String eventName = teamService.getEventName(eventCode);
            
            // DTO 변환
            List<TeamListRespDto.TeamItem> teamItems = teamPage.getContent().stream()
                .map(team -> TeamListRespDto.TeamItem.from(team, eventName))
                .collect(Collectors.toList());
            
            // 페이지네이션 정보
            PaginationDto pagination = PaginationDto.from(teamPage);
            
            // 응답 DTO 구성
            TeamListRespDto responseData = TeamListRespDto.builder()
                .content(teamItems)
                .pagination(pagination)
                .build();
            
            log.info("팀 목록 조회 완료 - 총 {}건, 현재페이지: {}/{}", 
                     teamPage.getTotalElements(), page + 1, teamPage.getTotalPages());
            
            return RespDto.success("팀 목록 조회 성공", responseData);
            
        } catch (Exception e) {
            log.error("팀 목록 조회 실패 - eventCode: {}", eventCode, e);
            return RespDto.fail("팀 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 팀 삭제 API
     * DELETE /api/v1/teams/{teamCode}
     */
    @DeleteMapping("/teams/{teamCode}")
    public RespDto<TeamDeleteRespDto> deleteTeam(@PathVariable("teamCode") Integer teamCode) {
        
        try {
            log.info("팀 삭제 요청 - teamCode: {}", teamCode);
            
            TeamService.TeamDeleteResult deleteResult = teamService.deleteTeam(teamCode);
            
            TeamDeleteRespDto responseData = TeamDeleteRespDto.from(
                deleteResult.getTeamCode(), 
                deleteResult.getDeletedTeamMembers()
            );
            
            log.info("팀 삭제 완료 - teamCode: {}, 삭제된 팀원 수: {}", 
                     deleteResult.getTeamCode(), deleteResult.getDeletedTeamMembers());
            
            return RespDto.success("팀이 성공적으로 삭제되었습니다.", responseData);
            
        } catch (IllegalArgumentException e) {
            log.warn("팀 삭제 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("팀 삭제 실패 - teamCode: {}", teamCode, e);
            return RespDto.fail("팀 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 팀 로그인 API
     * POST /api/v1/teams/login
     */
    @PostMapping("/teams/login")
    public RespDto<TeamLoginRespDto> loginTeam(@Valid @RequestBody TeamLoginReqDto request) {
        
        try {
            log.info("팀 로그인 요청 - teamId: {}", request.getTeamId());
            
            TeamMst team = teamService.loginTeam(request.getTeamId());
            
            TeamLoginRespDto responseData = TeamLoginRespDto.from(team);
            
            log.info("팀 로그인 성공 - teamId: {}, teamCode: {}, eventCode: {}", 
                     request.getTeamId(), team.getTeamCode(), team.getEventCode());
            
            return RespDto.success("로그인 성공", responseData);
            
        } catch (RuntimeException e) {
            log.warn("팀 로그인 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("팀 로그인 실패", e);
            return RespDto.fail("로그인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 팀 정보 수정 API
     * PUT /api/v1/teams/update
     */
    @PutMapping("/teams/update")
    public RespDto<Integer> updateTeam(@Valid @RequestBody TeamUpdateReqDto request) {
        
        try {
            log.info("팀 정보 수정 요청 - eventCode: {}, teamCode: {}, teamName: {}, 팀원 수: {}", 
                     request.getEventCode(), request.getTeamCode(), request.getTeamName(),
                     request.getMembers() != null ? request.getMembers().size() : 0);
            
            TeamMst updatedTeam = teamService.updateTeam(request);
            
            log.info("팀 정보 수정 완료 - teamCode: {}, teamName: {}", 
                     updatedTeam.getTeamCode(), updatedTeam.getTeamName());
            
            return RespDto.success("팀 정보가 성공적으로 수정되었습니다.", updatedTeam.getTeamCode());
            
        } catch (RuntimeException e) {
            log.warn("팀 정보 수정 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("팀 정보 수정 실패", e);
            return RespDto.fail("팀 정보 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
}
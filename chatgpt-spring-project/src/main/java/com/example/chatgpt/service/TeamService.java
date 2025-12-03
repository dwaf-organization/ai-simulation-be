package com.example.chatgpt.service;

import com.example.chatgpt.dto.team.reqDto.TeamCreateReqDto;
import com.example.chatgpt.entity.Event;
import com.example.chatgpt.entity.TeamMst;
import com.example.chatgpt.entity.CompanyCapabilityScore;
import com.example.chatgpt.repository.EventRepository;
import com.example.chatgpt.repository.TeamDtlRepository;
import com.example.chatgpt.repository.TeamMstRepository;
import com.example.chatgpt.repository.CompanyCapabilityScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeamService {
    
    private final TeamMstRepository teamMstRepository;
    private final TeamDtlRepository teamDtlRepository;
    private final EventRepository eventRepository;
    private final CompanyCapabilityScoreRepository companyCapabilityScoreRepository;
    
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 팀 생성
     */
    @Transactional
    public TeamMst createTeam(TeamCreateReqDto request) {
        log.info("팀 생성 요청 - eventCode: {}", request.getEventCode());
        
        // 1. 행사 존재 여부 확인
        Optional<Event> optionalEvent = eventRepository.findById(request.getEventCode());
        if (optionalEvent.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 행사입니다.");
        }
        
        // 2. 고유한 팀ID 생성
        String teamId = generateUniqueTeamId(request.getEventCode());
        
        // 3. 팀 생성 (기본값으로)
        TeamMst teamMst = TeamMst.builder()
            .eventCode(request.getEventCode())
            .teamId(teamId)
            .teamName("유니콘")  // 초기값 null
            .teamLeaderName(null)  // 초기값 null
            .teamImageUrl(null)  // 초기값 null
            .currentStageId(1)  // 기본값
            .currentStepId(1)   // 기본값
            .build();
        
        TeamMst savedTeam = teamMstRepository.save(teamMst);
        
        // 4. ✅ 역량 테이블 초기화 생성
        createInitialCapabilityScore(request.getEventCode(), savedTeam.getTeamCode());
        
        log.info("팀 생성 완료 - teamCode: {}, teamId: {}", savedTeam.getTeamCode(), savedTeam.getTeamId());
        
        return savedTeam;
    }
    
    /**
     * 역량 테이블 초기화 생성
     */
    private void createInitialCapabilityScore(Integer eventCode, Integer teamCode) {
        try {
            CompanyCapabilityScore capabilityScore = CompanyCapabilityScore.createDefault(eventCode, teamCode);
            companyCapabilityScoreRepository.save(capabilityScore);
            
            log.info("역량 테이블 초기화 완료 - eventCode: {}, teamCode: {}", eventCode, teamCode);
            
        } catch (Exception e) {
            log.error("역량 테이블 초기화 실패 - eventCode: {}, teamCode: {}", eventCode, teamCode, e);
            throw new RuntimeException("역량 테이블 생성에 실패했습니다.");
        }
    }
    
    /**
     * 팀 목록 조회 (특정 행사)
     */
    public Page<TeamMst> getTeamList(Integer eventCode, int page, int size) {
        log.info("팀 목록 조회 요청 - eventCode: {}, page: {}, size: {}", eventCode, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TeamMst> teamPage = teamMstRepository.findByEventCodeOrderByCreatedAtDesc(eventCode, pageable);
            
            log.info("팀 목록 조회 완료 - 총 {}건, 현재페이지: {}/{}", 
                     teamPage.getTotalElements(), page + 1, teamPage.getTotalPages());
            
            return teamPage;
            
        } catch (Exception e) {
            log.error("팀 목록 조회 실패 - eventCode: {}", eventCode, e);
            throw new RuntimeException("팀 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 팀 삭제 (팀원도 함께 삭제)
     */
    @Transactional
    public TeamDeleteResult deleteTeam(Integer teamCode) {
        log.info("팀 삭제 요청 - teamCode: {}", teamCode);
        
        // 1. 팀 존재 여부 확인
        Optional<TeamMst> optionalTeam = teamMstRepository.findById(teamCode);
        if (optionalTeam.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 팀입니다.");
        }
        
        // 2. ✅ 역량 테이블 삭제 (팀 삭제 전에 먼저 삭제)
        deleteCapabilityScore(teamCode);
        
        // 3. 팀원 삭제 (cascade)
        Integer deletedTeamMembers = teamDtlRepository.deleteByTeamCode(teamCode);
        
        log.info("팀원 삭제 완료 - teamCode: {}, 삭제된 팀원 수: {}", teamCode, deletedTeamMembers);
        
        // 4. 팀 삭제
        teamMstRepository.deleteById(teamCode);
        
        log.info("팀 삭제 완료 - teamCode: {}", teamCode);
        
        return new TeamDeleteResult(teamCode, deletedTeamMembers);
    }
    
    /**
     * 역량 테이블 삭제
     */
    private void deleteCapabilityScore(Integer teamCode) {
        try {
            companyCapabilityScoreRepository.deleteByTeamCode(teamCode);
            log.info("역량 테이블 삭제 완료 - teamCode: {}", teamCode);
            
        } catch (Exception e) {
            log.error("역량 테이블 삭제 실패 - teamCode: {}", teamCode, e);
            // 팀 삭제는 계속 진행 (역량 테이블 삭제 실패가 팀 삭제를 막지 않음)
        }
    }
    
    /**
     * 행사명 조회 (팀 목록에서 사용)
     */
    public String getEventName(Integer eventCode) {
        try {
            Optional<Event> optionalEvent = eventRepository.findById(eventCode);
            return optionalEvent.map(Event::getEventName).orElse("알 수 없는 행사");
        } catch (Exception e) {
            log.error("행사명 조회 실패 - eventCode: {}", eventCode, e);
            return "알 수 없는 행사";
        }
    }
    
    /**
     * 고유한 팀ID 생성 (영소문자+숫자 6자 + 행사코드)
     */
    private String generateUniqueTeamId(Integer eventCode) {
        String teamId;
        int maxAttempts = 100; // 무한루프 방지
        int attempts = 0;
        
        do {
            // 영소문자 + 숫자 6자 생성
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
            }
            
            // 행사코드 추가
            teamId = sb.toString() + eventCode;
            
            attempts++;
            
            if (attempts >= maxAttempts) {
                log.error("팀ID 생성 시도 횟수 초과 - eventCode: {}", eventCode);
                throw new RuntimeException("팀ID 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
            
        } while (teamMstRepository.existsByTeamId(teamId));
        
        log.debug("팀ID 생성 완료 - teamId: {}, 시도 횟수: {}", teamId, attempts);
        
        return teamId;
    }
    
    /**
     * 팀코드 존재 여부 확인
     */
    public boolean existsByTeamCode(Integer teamCode) {
        try {
            return teamMstRepository.existsById(teamCode);
        } catch (Exception e) {
            log.error("팀코드 존재 확인 실패 - teamCode: {}", teamCode, e);
            return false;
        }
    }
    
    /**
     * 팀 로그인 (teamId로 조회)
     */
    public TeamMst loginTeam(String teamId) {
        try {
            log.info("팀 로그인 요청 - teamId: {}", teamId);
            
            Optional<TeamMst> optionalTeam = teamMstRepository.findByTeamId(teamId);
            
            if (optionalTeam.isEmpty()) {
                throw new RuntimeException("존재하지 않는 팀 ID입니다.");
            }
            
            TeamMst team = optionalTeam.get();
            
            log.info("팀 로그인 성공 - teamId: {}, teamCode: {}, eventCode: {}", 
                     teamId, team.getTeamCode(), team.getEventCode());
            
            return team;
            
        } catch (Exception e) {
            log.error("팀 로그인 실패 - teamId: {}", teamId, e);
            throw new RuntimeException("팀 로그인 실패: " + e.getMessage());
        }
    }
    
    /**
     * 팀 삭제 결과 클래스
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TeamDeleteResult {
        private Integer teamCode;
        private Integer deletedTeamMembers;
    }
}
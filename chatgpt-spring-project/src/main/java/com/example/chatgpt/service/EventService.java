package com.example.chatgpt.service;

import com.example.chatgpt.dto.event.reqDto.EventCreateUpdateReqDto;
import com.example.chatgpt.dto.event.reqDto.EventDeleteReqDto;
import com.example.chatgpt.entity.Event;
import com.example.chatgpt.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {
    
    private final EventRepository eventRepository;
    
    /**
     * 행사 삭제 (하드 삭제)
     */
    @Transactional
    public List<Integer> deleteEvents(EventDeleteReqDto request) {
        log.info("행사 삭제 요청 - events: {}", request.getEvents());
        
        // 1. 기본 Validation
        if (request.isEmpty()) {
            throw new IllegalArgumentException("삭제할 행사를 선택해주세요.");
        }
        
        if (request.hasDuplicateEvents()) {
            throw new IllegalArgumentException("중복된 행사코드가 있습니다.");
        }
        
        List<Integer> eventCodes = request.getUniqueEvents();
        
        // 2. 존재하지 않는 행사 확인 (간단한 방식)
        List<Event> existingEvents = eventRepository.findByEventCodeIn(eventCodes);
        List<Integer> existingEventCodes = existingEvents.stream()
            .map(Event::getEventCode)
            .toList();
        
        // 요청된 코드 중 존재하지 않는 코드가 있는지 확인
        boolean hasNonExistentEvents = !existingEventCodes.containsAll(eventCodes);
        
        if (hasNonExistentEvents) {
            throw new IllegalArgumentException("존재하지 않는 행사입니다.");
        }
        
        // 3. 팀이 존재하는 행사 확인
        List<Event> eventsWithTeams = eventRepository.findEventsWithTeams(eventCodes);
        
        if (!eventsWithTeams.isEmpty()) {
            // 첫 번째로 발견된 팀이 있는 행사 정보로 에러 메시지 생성
            Event firstEventWithTeam = eventsWithTeams.get(0);
            String errorMessage = String.format("행사코드 %d번(%s)에 팀정보가 존재합니다.", 
                firstEventWithTeam.getEventCode(), 
                firstEventWithTeam.getEventName());
            
            log.warn("행사 삭제 실패 - 팀 존재: eventCode={}, eventName={}", 
                     firstEventWithTeam.getEventCode(), 
                     firstEventWithTeam.getEventName());
            
            throw new IllegalArgumentException(errorMessage);
        }
        
        // 4. 모든 검증 통과 시 삭제 실행
        log.info("행사 삭제 실행 - eventCodes: {}", eventCodes);
        eventRepository.deleteByEventCodeIn(eventCodes);
        
        log.info("행사 삭제 완료 - 삭제된 행사 수: {}, eventCodes: {}", eventCodes.size(), eventCodes);
        
        return eventCodes;
    }
    
    /**
     * 행사 생성 또는 수정
     */
    @Transactional
    public Event createOrUpdateEvent(EventCreateUpdateReqDto request) {
        // 날짜 형식 검증
        if (!request.isValidDateFormat()) {
            throw new IllegalArgumentException("올바른 날짜 형식이 아닙니다. (YYYY-MM-DD)");
        }
        
        if (request.isCreateRequest()) {
            return createEvent(request);
        } else {
            return updateEvent(request);
        }
    }
    
    /**
     * 행사 생성
     */
    private Event createEvent(EventCreateUpdateReqDto request) {
        log.info("행사 생성 요청 - 행사명: {}", request.getEventName());
        
        // 중복 체크
        if (eventRepository.existsByEventNameIgnoreCase(request.getEventName())) {
            throw new IllegalArgumentException("이미 존재하는 행사명입니다.");
        }
        
        // 기본값 설정
        String eventAt = request.getEventAt();
        if (eventAt == null || eventAt.trim().isEmpty()) {
            eventAt = LocalDate.now().toString();
        }
        
        // Event 엔티티 생성 (기본값 1로 고정)
        Event event = Event.builder()
            .eventName(request.getEventName())
            .eventAt(eventAt)
            .eventStatus(1) // 생성 시 항상 진행중
            .stageBatchProcess(1) // 기본값 1 고정
            .summaryViewProcess(1) // 기본값 1 고정
            .build();
        
        Event savedEvent = eventRepository.save(event);
        log.info("행사 생성 완료 - eventCode: {}", savedEvent.getEventCode());
        
        return savedEvent;
    }
    
    /**
     * 행사 수정
     */
    private Event updateEvent(EventCreateUpdateReqDto request) {
        log.info("행사 수정 요청 - eventCode: {}", request.getEventCode());
        
        // 존재하는 행사 확인
        Optional<Event> optionalEvent = eventRepository.findById(request.getEventCode());
        if (optionalEvent.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 행사입니다.");
        }
        
        Event existingEvent = optionalEvent.get();
        
        // 중복 체크 (본인 제외)
        if (eventRepository.existsByEventNameIgnoreCaseExcludingEventCode(
                request.getEventName(), request.getEventCode())) {
            throw new IllegalArgumentException("이미 존재하는 행사명입니다.");
        }
        
        // 기본값 설정
        String eventAt = request.getEventAt();
        if (eventAt == null || eventAt.trim().isEmpty()) {
            eventAt = existingEvent.getEventAt(); // 기존값 유지
        }
        
        // 엔티티 업데이트 (행사명과 날짜만 수정, stage/summary는 기존값 유지)
        existingEvent.setEventName(request.getEventName());
        existingEvent.setEventAt(eventAt);
        // stageBatchProcess와 summaryViewProcess는 기존값 유지
        // eventStatus는 그대로 유지 (별도 API에서 변경)
        
        Event savedEvent = eventRepository.save(existingEvent);
        log.info("행사 수정 완료 - eventCode: {}", savedEvent.getEventCode());
        
        return savedEvent;
    }
    
    /**
     * 행사 상태 토글 (1 ↔ 2)
     */
    @Transactional
    public Event toggleEventStatus(Integer eventCode) {
        log.info("행사 상태 변경 요청 - eventCode: {}", eventCode);
        
        // 존재하는 행사 확인
        Optional<Event> optionalEvent = eventRepository.findById(eventCode);
        if (optionalEvent.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 행사입니다.");
        }
        
        Event event = optionalEvent.get();
        Integer currentStatus = event.getEventStatus();
        
        // 상태 토글 (1 ↔ 2)
        Integer newStatus = (currentStatus == 1) ? 2 : 1;
        event.setEventStatus(newStatus);
        
        Event savedEvent = eventRepository.save(event);
        
        String statusMessage = (newStatus == 1) ? "진행중" : "종료";
        String prevStatusMessage = (currentStatus == 1) ? "진행중" : "종료";
        
        log.info("행사 상태 변경 완료 - eventCode: {}, {} → {}", 
                 eventCode, prevStatusMessage, statusMessage);
        
        return savedEvent;
    }
    
    /**
     * 상태 변경 메시지 생성
     */
    public String getStatusChangeMessage(Integer prevStatus, Integer newStatus) {
        String prevStatusText = (prevStatus == 1) ? "진행중" : "종료";
        String newStatusText = (newStatus == 1) ? "진행중" : "종료";
        
        return String.format("행사 상태가 변경되었습니다. (%s → %s)", prevStatusText, newStatusText);
    }
    
    /**
     * 다중 조건으로 이벤트 목록 조회 (페이지네이션)
     */
    public Page<Event> getEventList(
            Integer status,
            String startDate,
            String endDate,
            String eventName,
            int page,
            int size) {
        
        try {
            // 페이지네이션 설정 (0-based)
            Pageable pageable = PageRequest.of(page, size);
            
            // 상태 값 검증 및 기본값 설정 (0=전체, 1=진행중, 2=종료)
            Integer validStatus = (status != null && (status == 0 || status == 1 || status == 2)) ? status : 0;
            
            // 이벤트명 검색어 정리 (null이면 전체 검색)
            String searchEventName = (eventName != null && !eventName.trim().isEmpty()) ? eventName.trim() : null;
            
            // 날짜 형식 검증 (YYYY-MM-DD)
            String validStartDate = validateDateFormat(startDate);
            String validEndDate = validateDateFormat(endDate);
            
            log.info("이벤트 목록 조회 - status: {}, startDate: {}, endDate: {}, eventName: {}, page: {}, size: {}", 
                     validStatus, validStartDate, validEndDate, searchEventName, page, size);
            
            return eventRepository.findEventsWithFilters(
                validStatus, validStartDate, validEndDate, searchEventName, pageable);
            
        } catch (Exception e) {
            log.error("이벤트 목록 조회 실패", e);
            throw new RuntimeException("이벤트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 이벤트의 팀 수 조회
     */
    public Integer getTeamCountByEventCode(Integer eventCode) {
        try {
            return eventRepository.countTeamsByEventCode(eventCode);
        } catch (Exception e) {
            log.error("이벤트 {} 팀 수 조회 실패", eventCode, e);
            return 0; // 실패 시 0으로 반환
        }
    }
    
    /**
     * 날짜 형식 검증 (YYYY-MM-DD)
     */
    private String validateDateFormat(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dateStr.trim();
        
        // YYYY-MM-DD 형식 검증 (정규식)
        if (!trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            log.warn("잘못된 날짜 형식: {}. null로 처리", dateStr);
            return null;
        }
        
        return trimmed;
    }
    
    /**
     * 진행 중인 이벤트만 조회
     */
    public Page<Event> getInProgressEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByEventStatusOrderByEventAtDesc(1, pageable);
    }
    
    /**
     * 완료된 이벤트만 조회
     */
    public Page<Event> getCompletedEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByEventStatusOrderByEventAtDesc(2, pageable);
    }
    
    /**
     * 최근 이벤트 조회
     */
    public Page<Event> getRecentEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findRecentEvents(pageable);
    }
    
    /**
     * 행사코드 존재 여부 확인
     */
    public boolean existsByEventCode(Integer eventCode) {
        try {
            return eventRepository.existsById(eventCode);
        } catch (Exception e) {
            log.error("행사코드 존재 확인 실패 - eventCode: {}", eventCode, e);
            return false;
        }
    }
}
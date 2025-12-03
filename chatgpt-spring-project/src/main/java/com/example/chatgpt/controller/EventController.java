package com.example.chatgpt.controller;

import com.example.chatgpt.common.dto.RespDto;
import com.example.chatgpt.common.dto.PaginationDto;
import com.example.chatgpt.dto.event.reqDto.EventCreateUpdateReqDto;
import com.example.chatgpt.dto.event.reqDto.EventDeleteReqDto;
import com.example.chatgpt.dto.event.respDto.EventCreateUpdateRespDto;
import com.example.chatgpt.dto.event.respDto.EventDeleteRespDto;
import com.example.chatgpt.dto.event.respDto.EventListRespDto;
import com.example.chatgpt.dto.event.respDto.EventStatusChangeRespDto;
import com.example.chatgpt.entity.Event;
import com.example.chatgpt.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EventController {
    
    private final EventService eventService;
    
    /**
     * 행사 생성/수정 API
     * POST /api/v1/events/save
     */
    @PostMapping("/events/save")
    public RespDto<EventCreateUpdateRespDto> createOrUpdateEvent(
            @Valid @RequestBody EventCreateUpdateReqDto request) {
        
        try {
            log.info("행사 {}} 요청 - 행사명: {}, eventCode: {}", 
                     request.isCreateRequest() ? "생성" : "수정",
                     request.getEventName(), 
                     request.getEventCode());
            
            Event savedEvent = eventService.createOrUpdateEvent(request);
            
            EventCreateUpdateRespDto responseData = EventCreateUpdateRespDto.from(savedEvent.getEventCode());
            
            String message = request.isCreateRequest() ? 
                "행사가 성공적으로 생성되었습니다." : "행사가 성공적으로 수정되었습니다.";
            
            log.info("행사 {} 완료 - eventCode: {}", 
                     request.isCreateRequest() ? "생성" : "수정", 
                     savedEvent.getEventCode());
            
            return RespDto.success(message, responseData);
            
        } catch (IllegalArgumentException e) {
            log.warn("행사 생성/수정 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("행사 생성/수정 실패", e);
            return RespDto.fail("행사 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 행사 삭제 API (하드 삭제)
     * DELETE /api/v1/events/delete
     */
    @DeleteMapping("/events/delete")
    public RespDto<EventDeleteRespDto> deleteEvents(@Valid @RequestBody EventDeleteReqDto request) {
        
        try {
            log.info("행사 삭제 요청 - events: {}", request.getEvents());
            
            List<Integer> deletedEventCodes = eventService.deleteEvents(request);
            
            EventDeleteRespDto responseData = EventDeleteRespDto.from(deletedEventCodes);
            
            log.info("행사 삭제 완료 - 삭제된 행사 수: {}, eventCodes: {}", 
                     deletedEventCodes.size(), deletedEventCodes);
            
            return RespDto.success("행사가 성공적으로 삭제되었습니다.", responseData);
            
        } catch (IllegalArgumentException e) {
            log.warn("행사 삭제 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("행사 삭제 실패", e);
            return RespDto.fail("행사 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 행사 상태 변경 API (토글)
     * POST /api/v1/events/status/{eventCode}
     */
    @PostMapping("/events/status/{eventCode}")
    public RespDto<EventStatusChangeRespDto> toggleEventStatus(@PathVariable("eventCode") Integer eventCode) {
        
        try {
            log.info("행사 상태 변경 요청 - eventCode: {}", eventCode);
            
            // 현재 상태 조회 (메시지 생성용)
            Event currentEvent = eventService.getEventList(0, null, null, null, 0, 1)
                .getContent().stream()
                .filter(event -> event.getEventCode().equals(eventCode))
                .findFirst()
                .orElse(null);
            
            Integer prevStatus = (currentEvent != null) ? currentEvent.getEventStatus() : null;
            
            // 상태 토글
            Event updatedEvent = eventService.toggleEventStatus(eventCode);
            
            EventStatusChangeRespDto responseData = EventStatusChangeRespDto.from(
                updatedEvent.getEventCode(), updatedEvent.getEventStatus());
            
            String message = (prevStatus != null) ? 
                eventService.getStatusChangeMessage(prevStatus, updatedEvent.getEventStatus()) :
                "행사 상태가 변경되었습니다.";
            
            log.info("행사 상태 변경 완료 - eventCode: {}, 새 상태: {}", 
                     eventCode, updatedEvent.getEventStatus());
            
            return RespDto.success(message, responseData);
            
        } catch (IllegalArgumentException e) {
            log.warn("행사 상태 변경 검증 실패: {}", e.getMessage());
            return RespDto.fail(e.getMessage());
            
        } catch (Exception e) {
            log.error("행사 상태 변경 실패", e);
            return RespDto.fail("행사 상태 변경 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 이벤트 목록 조회 (다중 조건 검색 + 페이지네이션)
     * GET /api/v1/events?status={status}&startDate={startDate}&endDate={endDate}&eventName={eventName}&page={page}&size={size}
     */
    @GetMapping("/events")
    public RespDto<EventListRespDto> getEventList(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "eventName", required = false) String eventName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            log.info("이벤트 목록 조회 요청 - status: {}, startDate: {}, endDate: {}, eventName: {}, page: {}, size: {}", 
                     status, startDate, endDate, eventName, page, size);
            
            // 이벤트 목록 조회
            Page<Event> eventPage = eventService.getEventList(status, startDate, endDate, eventName, page, size);
            
            // 각 이벤트의 팀 수 조회 및 DTO 변환
            List<EventListRespDto.EventItem> eventItems = eventPage.getContent().stream()
                .map(event -> {
                    Integer teamCount = eventService.getTeamCountByEventCode(event.getEventCode());
                    return EventListRespDto.EventItem.from(event, teamCount);
                })
                .collect(Collectors.toList());
            
            // 페이지네이션 정보
            PaginationDto pagination = PaginationDto.from(eventPage);
            
            // 응답 DTO 구성
            EventListRespDto responseData = EventListRespDto.builder()
                .content(eventItems)
                .pagination(pagination)
                .build();
            
            log.info("이벤트 목록 조회 완료 - 총 {}건, 현재페이지: {}/{}", 
                     eventPage.getTotalElements(), page + 1, eventPage.getTotalPages());
            
            return RespDto.success("행사 목록 조회 성공", responseData);
            
        } catch (Exception e) {
            log.error("이벤트 목록 조회 실패", e);
            return RespDto.fail("행사 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 진행 중인 이벤트만 조회
     * GET /api/v1/events/in-progress?page={page}&size={size}
     */
    @GetMapping("/events/in-progress")
    public RespDto<EventListRespDto> getInProgressEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            Page<Event> eventPage = eventService.getInProgressEvents(page, size);
            
            List<EventListRespDto.EventItem> eventItems = eventPage.getContent().stream()
                .map(event -> {
                    Integer teamCount = eventService.getTeamCountByEventCode(event.getEventCode());
                    return EventListRespDto.EventItem.from(event, teamCount);
                })
                .collect(Collectors.toList());
            
            EventListRespDto responseData = EventListRespDto.builder()
                .content(eventItems)
                .pagination(PaginationDto.from(eventPage))
                .build();
            
            return RespDto.success("진행 중인 행사 목록 조회 성공", responseData);
            
        } catch (Exception e) {
            log.error("진행 중인 이벤트 조회 실패", e);
            return RespDto.fail("진행 중인 행사 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 완료된 이벤트만 조회  
     * GET /api/v1/events/completed?page={page}&size={size}
     */
    @GetMapping("/events/completed")
    public RespDto<EventListRespDto> getCompletedEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            Page<Event> eventPage = eventService.getCompletedEvents(page, size);
            
            List<EventListRespDto.EventItem> eventItems = eventPage.getContent().stream()
                .map(event -> {
                    Integer teamCount = eventService.getTeamCountByEventCode(event.getEventCode());
                    return EventListRespDto.EventItem.from(event, teamCount);
                })
                .collect(Collectors.toList());
            
            EventListRespDto responseData = EventListRespDto.builder()
                .content(eventItems)
                .pagination(PaginationDto.from(eventPage))
                .build();
            
            return RespDto.success("완료된 행사 목록 조회 성공", responseData);
            
        } catch (Exception e) {
            log.error("완료된 이벤트 조회 실패", e);
            return RespDto.fail("완료된 행사 목록 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 최근 이벤트 조회
     * GET /api/v1/events/recent?page={page}&size={size}
     */
    @GetMapping("/events/recent")
    public RespDto<EventListRespDto> getRecentEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            Page<Event> eventPage = eventService.getRecentEvents(page, size);
            
            List<EventListRespDto.EventItem> eventItems = eventPage.getContent().stream()
                .map(event -> {
                    Integer teamCount = eventService.getTeamCountByEventCode(event.getEventCode());
                    return EventListRespDto.EventItem.from(event, teamCount);
                })
                .collect(Collectors.toList());
            
            EventListRespDto responseData = EventListRespDto.builder()
                .content(eventItems)
                .pagination(PaginationDto.from(eventPage))
                .build();
            
            return RespDto.success("최근 행사 목록 조회 성공", responseData);
            
        } catch (Exception e) {
            log.error("최근 이벤트 조회 실패", e);
            return RespDto.fail("최근 행사 목록 조회 실패: " + e.getMessage());
        }
    }
}
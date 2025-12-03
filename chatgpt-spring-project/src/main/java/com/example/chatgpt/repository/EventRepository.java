package com.example.chatgpt.repository;

import com.example.chatgpt.entity.Event;
import com.example.chatgpt.entity.TeamMst;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    
    /**
     * 다중 조건으로 이벤트 검색 (페이지네이션)
     * team_count는 서브쿼리로 계산
     */
    @Query("SELECT e FROM Event e " +
           "WHERE (:status = 0 OR e.eventStatus = :status) " +
           "AND (:startDate IS NULL OR e.eventAt >= :startDate) " +
           "AND (:endDate IS NULL OR e.eventAt <= :endDate) " +
           "AND (:eventName IS NULL OR e.eventName LIKE %:eventName%) " +
           "ORDER BY e.eventAt DESC, e.eventCode DESC")
    Page<Event> findEventsWithFilters(
        @Param("status") Integer status,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate, 
        @Param("eventName") String eventName,
        Pageable pageable
    );
    
    /**
     * 특정 이벤트의 팀 수 조회
     */
    @Query("SELECT COUNT(t) FROM TeamMst t WHERE t.eventCode = :eventCode")
    Integer countTeamsByEventCode(@Param("eventCode") Integer eventCode);
    
    /**
     * 행사명 중복 체크 (대소문자 무시, 본인 제외)
     */
    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE LOWER(e.eventName) = LOWER(:eventName) " +
           "AND (:eventCode IS NULL OR e.eventCode != :eventCode)")
    boolean existsByEventNameIgnoreCaseExcludingEventCode(
        @Param("eventName") String eventName, 
        @Param("eventCode") Integer eventCode);
    
    /**
     * 행사명 중복 체크 (생성 시)
     */
    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE LOWER(e.eventName) = LOWER(:eventName)")
    boolean existsByEventNameIgnoreCase(@Param("eventName") String eventName);
    
    /**
     * 여러 이벤트 코드로 이벤트 조회 (삭제용)
     */
    List<Event> findByEventCodeIn(List<Integer> eventCodes);
    
    /**
     * 특정 이벤트 코드들에 대해 팀이 존재하는 이벤트 조회
     * (eventCode와 eventName을 함께 반환)
     */
    @Query("SELECT e FROM Event e WHERE e.eventCode IN :eventCodes " +
           "AND EXISTS (SELECT 1 FROM TeamMst t WHERE t.eventCode = e.eventCode)")
    List<Event> findEventsWithTeams(@Param("eventCodes") List<Integer> eventCodes);
    
    /**
     * 여러 이벤트를 한 번에 삭제
     */
    void deleteByEventCodeIn(List<Integer> eventCodes);
    
    /**
     * 진행 중인 이벤트만 조회
     */
    Page<Event> findByEventStatusOrderByEventAtDesc(Integer eventStatus, Pageable pageable);
    
    /**
     * 이벤트명으로 검색
     */
    Page<Event> findByEventNameContainingIgnoreCaseOrderByEventAtDesc(String eventName, Pageable pageable);
    
    /**
     * 날짜 범위로 검색
     */
    Page<Event> findByEventAtBetweenOrderByEventAtDesc(String startDate, String endDate, Pageable pageable);
    
    /**
     * 최근 이벤트 조회
     */
    @Query("SELECT e FROM Event e ORDER BY e.createdAt DESC")
    Page<Event> findRecentEvents(Pageable pageable);
}
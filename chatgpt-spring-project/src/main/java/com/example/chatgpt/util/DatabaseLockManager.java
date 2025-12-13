package com.example.chatgpt.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 데이터베이스 작업 Lock 관리자
 * 서비스별 우선순위 기반 동시성 제어
 */
@Component
@Slf4j
public class DatabaseLockManager {
    
    // 서비스별 우선순위 락 (순서대로 실행되도록 보장)
    private static final Object STAGE_SUMMARY_LOCK = new Object();
    private static final Object FINANCIAL_STATEMENT_LOCK = new Object();
    private static final Object OPERATING_EXPENSE_LOCK = new Object();
    private static final Object BIZPLAN_LOCK = new Object();
    private static final Object TEAM_MANAGEMENT_LOCK = new Object();
    private static final Object DEFAULT_LOCK = new Object();
    
    /**
     * 서비스 타입별로 적절한 Lock과 함께 작업 실행 (void 메서드용)
     */
    public void executeWithLock(ServiceType serviceType, Runnable operation) {
        Object lock = getLockByServiceType(serviceType);
        String serviceName = serviceType.name();
        
        long startTime = System.currentTimeMillis();
        long timeoutMs = 60000; // 60초 타임아웃
        
        synchronized(lock) {
            try {
                // 타임아웃 체크
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    throw new RuntimeException("Lock 대기 시간 초과: " + serviceName);
                }
                
                log.debug("DB Lock 획득: {}", serviceName);
                operation.run();
                
            } catch (Exception e) {
            	if (isDeadlockException(e) || isTimeoutException(e)) {
                    log.warn("데드락 감지 후 재시도: {} - {}", serviceName, e.getMessage());
                    
                    try {
                        // 랜덤 지연
                        Thread.sleep(200 + (long)(Math.random() * 300));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    
                    operation.run(); // 재시도
                } else {
                    throw new RuntimeException("DB 작업 실패: " + serviceName, e);
                }
                
            } finally {
                log.debug("DB Lock 해제: {}", serviceName);
            }
        }
    }
    
    private boolean isTimeoutException(Throwable e) {
        String message = e.getMessage();
        if (message == null) return false;
        
        return message.toLowerCase().contains("timeout") ||
               message.toLowerCase().contains("expired");
    }
    
    /**
     * 서비스 타입별로 적절한 Lock과 함께 작업 실행 (값 반환용)
     */
    public <T> T executeWithLock(ServiceType serviceType, Supplier<T> operation) {
        Object lock = getLockByServiceType(serviceType);
        String serviceName = serviceType.name();
        
        synchronized(lock) {
            try {
                log.debug("DB Lock 획득: {}", serviceName);
                return operation.get();
                
            } catch (Exception e) {
                if (isDeadlockException(e)) {
                    log.warn("데드락 감지 후 재시도: {} - {}", serviceName, e.getMessage());
                    
                    try {
                        // 랜덤 지연
                        Thread.sleep(200 + (long)(Math.random() * 300));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    
                    return operation.get(); // 재시도
                } else {
                    throw new RuntimeException("DB 작업 실패: " + serviceName, e);
                }
                
            } finally {
                log.debug("DB Lock 해제: {}", serviceName);
            }
        }
    }
    
    /**
     * 서비스 타입별 Lock 반환
     */
    private Object getLockByServiceType(ServiceType serviceType) {
        switch (serviceType) {
            case STAGE_SUMMARY: return STAGE_SUMMARY_LOCK;
            case FINANCIAL_STATEMENT: return FINANCIAL_STATEMENT_LOCK;
            case OPERATING_EXPENSE: return OPERATING_EXPENSE_LOCK;
            case BIZPLAN: return BIZPLAN_LOCK;
            case TEAM_MANAGEMENT: return TEAM_MANAGEMENT_LOCK;
            default: return DEFAULT_LOCK;
        }
    }
    
    /**
     * 데드락 관련 예외인지 확인
     */
    private boolean isDeadlockException(Throwable e) {
        String message = e.getMessage();
        if (message == null) return false;
        
        return message.toLowerCase().contains("deadlock") ||
               message.contains("1213") ||  // MySQL 데드락 에러 코드
               message.contains("40001") || // SQL State 데드락
               message.toLowerCase().contains("lock wait timeout");
    }
    
    /**
     * 서비스 타입 enum
     */
    public enum ServiceType {
        STAGE_SUMMARY,        // StageSummaryGeneratorService
        FINANCIAL_STATEMENT,  // 재무제표 관련 서비스들
        OPERATING_EXPENSE,    // OperatingExpenseService
        BIZPLAN,             // Stage1BizplanService 등
        TEAM_MANAGEMENT,     // 팀/이벤트 관리
        DEFAULT              // 기타
    }
}
package com.example.chatgpt.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전역 데드락 방지 AOP
 */
@Component
@Aspect
@Slf4j
public class DeadlockPreventionAspect {
    
    // 전역 락 (모든 DB 작업을 순차적으로 처리)
    private static final Object GLOBAL_DB_LOCK = new Object();
    
    /**
     * @Transactional 어노테이션이 있는 모든 메서드에 데드락 방지 적용
     */
    @Around("@annotation(transactional)")
    public Object preventDeadlock(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        synchronized(GLOBAL_DB_LOCK) {
            try {
                log.debug("데드락 방지 Lock 획득: {}.{}", className, methodName);
                return joinPoint.proceed();
                
            } catch (Exception e) {
                if (isDeadlockException(e)) {
                    log.warn("데드락 감지됨, 재시도: {}.{} - {}", className, methodName, e.getMessage());
                    
                    // 랜덤 지연 후 재시도
                    Thread.sleep(100 + (long)(Math.random() * 200));
                    return joinPoint.proceed();
                }
                throw e;
                
            } finally {
                log.debug("데드락 방지 Lock 해제: {}.{}", className, methodName);
            }
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
               message.contains("40001");   // SQL State 데드락
    }
}
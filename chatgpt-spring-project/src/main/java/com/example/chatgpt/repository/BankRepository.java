package com.example.chatgpt.repository;

import com.example.chatgpt.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Integer> {
    
    /**
     * 모든 은행 목록 조회 (bankCode 순)
     */
    List<Bank> findAllByOrderByBankCodeAsc();
    
    /**
     * bankCode와 bankName만 조회하는 프로젝션 쿼리
     */
    @Query("SELECT b.bankCode, b.bankName FROM Bank b ORDER BY b.bankCode ASC")
    List<Object[]> findBankCodeAndNameOnly();
    
    /**
     * 은행 코드로 은행 정보 조회
     */
    Optional<Bank> findByBankCode(Integer bankCode);
    
    /**
     * 은행명으로 은행 정보 조회
     */
    Optional<Bank> findByBankName(String bankName);
    
}
package com.example.chatgpt.service;

import com.example.chatgpt.dto.bank.respDto.BankListRespDto;
import com.example.chatgpt.entity.Bank;
import com.example.chatgpt.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BankService {
    
    private final BankRepository bankRepository;
    
    /**
     * 전체 은행 목록 조회
     */
    public List<BankListRespDto> getBankList() {
        log.info("은행 목록 조회 요청");
        
        try {
            List<Bank> banks = bankRepository.findAllByOrderByBankCodeAsc();
            
            List<BankListRespDto> bankList = banks.stream()
                    .map(BankListRespDto::fromEntity)
                    .toList();
            
            log.info("은행 목록 조회 완료 - {}개 은행", bankList.size());
            return bankList;
            
        } catch (Exception e) {
            log.error("은행 목록 조회 실패", e);
            throw new RuntimeException("은행 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
package com.example.chatgpt.service;

import com.example.chatgpt.dto.loaninfo.reqDto.LoanInfoCreateReqDto;
import com.example.chatgpt.entity.LoanInfo;
import com.example.chatgpt.repository.LoanInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LoanInfoService {
    
    private final LoanInfoRepository loanInfoRepository;
    
    /**
     * 대출정보 생성 또는 업데이트 (중복시 덮어쓰기)
     */
    @Transactional
    public Integer createOrUpdateLoanInfo(LoanInfoCreateReqDto request) {
        try {
            // 1. String 데이터를 Integer로 변환
            Integer eventCode = Integer.valueOf(request.getEventCode());
            Integer teamCode = Integer.valueOf(request.getTeamCode());
            Integer stageStep = Integer.valueOf(request.getStageStep());
            Integer bankCode = Integer.valueOf(request.getBankCode());
            
            log.info("대출정보 생성/업데이트 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            // 2. 기존 데이터 확인 (중복 체크)
            Optional<LoanInfo> existingLoan = loanInfoRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            LoanInfo loanInfo;
            
            if (existingLoan.isPresent()) {
                // 3. 기존 데이터가 있으면 업데이트 (덮어쓰기)
                loanInfo = existingLoan.get();
                updateLoanInfoFields(loanInfo, request, bankCode);
                log.info("기존 대출정보 업데이트 - loanCode: {}", loanInfo.getLoanCode());
                
            } else {
                // 4. 새로운 데이터 생성
                loanInfo = createNewLoanInfo(request, eventCode, teamCode, stageStep, bankCode);
                log.info("새 대출정보 생성");
            }
            
            // 5. 저장
            LoanInfo savedLoan = loanInfoRepository.save(loanInfo);
            
            log.info("대출정보 저장 완료 - loanCode: {}", savedLoan.getLoanCode());
            return savedLoan.getLoanCode();
            
        } catch (NumberFormatException e) {
            log.error("숫자 변환 실패", e);
            throw new RuntimeException("잘못된 숫자 형식입니다: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("대출정보 생성/업데이트 실패", e);
            throw new RuntimeException("대출정보 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 기존 대출정보 필드 업데이트
     */
    private void updateLoanInfoFields(LoanInfo loanInfo, LoanInfoCreateReqDto request, Integer bankCode) {
        loanInfo.setBankCode(bankCode);
        loanInfo.setLoanType(request.getLoanType());
        loanInfo.setBizRegDocPath(request.getBizRegDocPath());
        loanInfo.setCorpRegDocPath(request.getCorpRegDocPath());
        loanInfo.setShareholderListDocPath(request.getShareholderListDocPath());
        loanInfo.setFinancialStatementDocPath(request.getFinancialStatementDocPath());
        loanInfo.setVatTaxDocPath(request.getVatTaxDocPath());
        loanInfo.setSocialInsuranceDocPath(request.getSocialInsuranceDocPath());
        loanInfo.setTaxPaymentDocPath(request.getTaxPaymentDocPath());
    }
    
    /**
     * 새 대출정보 생성
     */
    private LoanInfo createNewLoanInfo(LoanInfoCreateReqDto request, Integer eventCode, 
                                     Integer teamCode, Integer stageStep, Integer bankCode) {
        return LoanInfo.builder()
            .eventCode(eventCode)
            .teamCode(teamCode)
            .stageStep(stageStep)
            .bankCode(bankCode)
            .loanType(request.getLoanType())
            .bizRegDocPath(request.getBizRegDocPath())
            .corpRegDocPath(request.getCorpRegDocPath())
            .shareholderListDocPath(request.getShareholderListDocPath())
            .financialStatementDocPath(request.getFinancialStatementDocPath())
            .vatTaxDocPath(request.getVatTaxDocPath())
            .socialInsuranceDocPath(request.getSocialInsuranceDocPath())
            .taxPaymentDocPath(request.getTaxPaymentDocPath())
            .build();
    }
}
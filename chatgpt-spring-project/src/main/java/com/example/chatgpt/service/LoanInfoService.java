package com.example.chatgpt.service;

import com.example.chatgpt.dto.loaninfo.reqDto.LoanInfoCreateReqDto;
import com.example.chatgpt.dto.loaninfo.respDto.LoanInfoDto;
import com.example.chatgpt.dto.loaninfo.respDto.LoanInfoListRespDto;
import com.example.chatgpt.entity.Bank;
import com.example.chatgpt.entity.FinancialStatement;
import com.example.chatgpt.entity.LoanBusinessPlan;
import com.example.chatgpt.entity.LoanInfo;
import com.example.chatgpt.repository.BankRepository;
import com.example.chatgpt.repository.FinancialStatementRepository;
import com.example.chatgpt.repository.LoanBusinessPlanRepository;
import com.example.chatgpt.repository.LoanInfoRepository;
import com.example.chatgpt.util.DatabaseLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LoanInfoService {
    
    private final LoanInfoRepository loanInfoRepository;
    private final BankRepository bankRepository;
    private final FinancialStatementRepository financialStatementRepository;
    private final LoanBusinessPlanRepository loanBusinessPlanRepository;
    private final DatabaseLockManager lockManager;  // Lock Manager 추가
    
    /**
     * 대출정보 목록 조회 (은행명 포함)
     */
    public LoanInfoListRespDto getLoanInfoList(Integer eventCode, Integer teamCode, Integer stageStep) {
        try {
            log.info("대출정보 목록 조회 - eventCode: {}, teamCode: {}, stageStep: {}", 
                     eventCode, teamCode, stageStep);
            
            // 1. 대출정보 조회
            Optional<LoanInfo> optionalLoanInfo = loanInfoRepository
                .findByEventCodeAndTeamCodeAndStageStep(eventCode, teamCode, stageStep);
            
            if (optionalLoanInfo.isEmpty()) {
                log.info("대출정보가 없음 - eventCode: {}, teamCode: {}, stageStep: {}", 
                         eventCode, teamCode, stageStep);
                return LoanInfoListRespDto.empty();
            }
            
            LoanInfo loanInfo = optionalLoanInfo.get();
            
            // 2. 은행 정보 조회
            String bankName = getBankName(loanInfo.getBankCode());
            
            // 3. DTO 변환
            LoanInfoDto loanInfoDto = LoanInfoDto.from(loanInfo, bankName);
            
            log.info("대출정보 조회 완료 - loanCode: {}, bank: {}", 
                     loanInfo.getLoanCode(), bankName);
            
            return LoanInfoListRespDto.from(List.of(loanInfoDto));
            
        } catch (Exception e) {
            log.error("대출정보 목록 조회 실패", e);
            throw new RuntimeException("대출정보 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 은행 코드로 은행명 조회
     */
    private String getBankName(Integer bankCode) {
        try {
            Optional<Bank> bankOpt = bankRepository.findByBankCode(bankCode);
            return bankOpt.map(Bank::getBankName).orElse("알 수 없는 은행");
        } catch (Exception e) {
            log.warn("은행 정보 조회 실패 - bankCode: {}", bankCode, e);
            return "알 수 없는 은행";
        }
    }
    

    /**
     * 대출정보 생성 또는 업데이트 (중복시 덮어쓰기) (데드락 방지 적용)
     */
    @Transactional
    public Integer createOrUpdateLoanInfo(LoanInfoCreateReqDto request) {
        log.info("대출정보 생성/업데이트 요청 - eventCode: {}, teamCode: {}, stageStep: {}", 
                 request.getEventCode(), request.getTeamCode(), request.getStageStep());
        
        // 데드락 방지를 위한 Lock 적용
        return lockManager.executeWithLock(DatabaseLockManager.ServiceType.FINANCIAL_STATEMENT, () -> {
            try {
                // 2. 기존 데이터 확인 (중복 체크)
                Optional<LoanInfo> existingLoan = loanInfoRepository
                    .findByEventCodeAndTeamCodeAndStageStep(
                        request.getEventCode(), request.getTeamCode(), request.getStageStep());
                
                LoanInfo loanInfo;
                
                if (existingLoan.isPresent()) {
                    // 3. 기존 데이터가 있으면 업데이트 (덮어쓰기)
                    loanInfo = existingLoan.get();
                    updateLoanInfoFields(loanInfo, request);
                    log.info("기존 대출정보 업데이트 - loanCode: {}", loanInfo.getLoanCode());
                    
                } else {
                    // 4. 새로운 데이터 생성
                    loanInfo = createNewLoanInfo(request);
                    log.info("새 대출정보 생성");
                }
                
                // 5. 저장
                LoanInfo savedLoan = loanInfoRepository.save(loanInfo);
                
                log.info("대출정보 저장 완료 - loanCode: {}", savedLoan.getLoanCode());
                return savedLoan.getLoanCode();
                
            } catch (Exception e) {
                log.error("대출정보 생성/업데이트 실패", e);
                throw new RuntimeException("대출정보 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }
    
    /**
     * 기존 대출정보 필드 업데이트
     */
    private void updateLoanInfoFields(LoanInfo loanInfo, LoanInfoCreateReqDto request) {
        loanInfo.setBankCode(request.getBankCode());
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
    private LoanInfo createNewLoanInfo(LoanInfoCreateReqDto request) {
        return LoanInfo.builder()
            .eventCode(request.getEventCode())
            .teamCode(request.getTeamCode())
            .stageStep(request.getStageStep())
            .bankCode(request.getBankCode())
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
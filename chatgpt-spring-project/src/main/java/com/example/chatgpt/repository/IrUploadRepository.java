package com.example.chatgpt.repository;

import com.example.chatgpt.entity.IrUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IrUploadRepository extends JpaRepository<IrUpload, Integer> {
    
    /**
     * 특정 이벤트-팀의 IR 자료 조회 (중복 체크용)
     */
    Optional<IrUpload> findByEventCodeAndTeamCode(Integer eventCode, Integer teamCode);
    
    /**
     * 특정 이벤트의 모든 IR 자료 조회
     */
    List<IrUpload> findByEventCodeOrderByCreatedAtDesc(Integer eventCode);
    
    /**
     * 중복 존재 여부 확인
     */
    boolean existsByEventCodeAndTeamCode(Integer eventCode, Integer teamCode);
}
package com.example.chatgpt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FileTestController {
    
    /**
     * 파일 업로드 테스트 페이지
     * GET /fileTest
     */
    @GetMapping("/fileTest")
    public String fileTestPage() {
        return "Filetest";  // templates/fileTest.html
    }
    
    /**
     * IR 파일 업로드 테스트 페이지
     * GET /irFileTest
     */
    @GetMapping("/irFileTest")
    public String irFileTestPage() {
        return "IrFileTest";  // templates/IrFileTest.html
    }
    
    /**
     * Stage6 사업계획서 파싱 테스트 페이지
     * GET /stage6FileTest
     */
    @GetMapping("/stage6FileTest")
    public String stage6FileTestPage() {
        return "Stage6FileTest";  // templates/Stage6FileTest.html
    }
}
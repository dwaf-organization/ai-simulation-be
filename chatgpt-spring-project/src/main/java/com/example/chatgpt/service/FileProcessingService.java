package com.example.chatgpt.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// HWP 처리용 라이브러리 추가
//import kr.dogfoot.hwplib.object.HWPFile;
//import kr.dogfoot.hwplib.reader.HWPReader;
//import kr.dogfoot.hwplib.object.bodytext.Section;
//import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
/**
 * 파일 처리 서비스 (이미지/표 추출 개선 버전 + HWP 지원)
 */
@Service
@Slf4j
public class FileProcessingService {

    /**
     * 파일 형식 검증 (HWP 추가)
     */
    public boolean isValidFileFormat(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".pdf") || 
               lowerFilename.endsWith(".docx")
//               || 
//               lowerFilename.endsWith(".hwp")
               ;
    }

    /**
     * 파일에서 텍스트 추출 (이미지/표 정보 포함 + HWP 지원)
     */
    public String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        
        if (filename == null) {
            throw new IOException("파일 이름이 없습니다.");
        }

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".pdf")) {
            return extractTextFromPDF(file);
        } else if (lowerFilename.endsWith(".docx")) {
            return extractTextFromDOCX(file);
//        } else if (lowerFilename.endsWith(".hwp")) {
//            return extractTextFromHWP(file);  // HWP 처리 추가
        } else {
            throw new IOException("지원하지 않는 파일 형식입니다: " + filename);
        }
    }
//
//    /**
//     * HWP에서 텍스트 추출 (실제 API 구조에 맞춤)
//     */
//    private String extractTextFromHWP(MultipartFile file) throws IOException {
//        StringBuilder result = new StringBuilder();
//        
//        try (InputStream inputStream = file.getInputStream()) {
//            HWPFile hwpFile = HWPReader.fromInputStream(inputStream);
//            
//            // 모든 섹션을 순회하면서 텍스트 추출
//            if (hwpFile.getBodyText() != null && hwpFile.getBodyText().getSectionList() != null) {
//                for (Section section : hwpFile.getBodyText().getSectionList()) {
//                    extractTextFromSection(section, result);
//                }
//            }
//            
//            log.info("HWP 추출 완료: 약 {}자", result.length());
//            
//        } catch (Exception e) {
//            log.error("HWP 텍스트 추출 실패", e);
//            throw new IOException("HWP 파일을 읽을 수 없습니다: " + e.getMessage());
//        }
//        
//        return result.toString();
//    }
//    
//    /**
//     * HWP 섹션에서 텍스트 추출 (실제 API 구조 반영)
//     */
//    private void extractTextFromSection(Section section, StringBuilder textBuilder) {
//        try {
//            // 섹션 내 문단 개수 확인 후 순회
//            int paragraphCount = section.getParagraphCount();
//            
//            for (int i = 0; i < paragraphCount; i++) {
//                try {
//                    Paragraph paragraph = section.getParagraph(i); // 인덱스로 접근
//                    
//                    if (paragraph != null) {
//                        // 문단에서 텍스트 추출 (단순화된 방식)
//                        String paragraphText = extractTextFromParagraph(paragraph);
//                        if (paragraphText != null && !paragraphText.trim().isEmpty()) {
//                            textBuilder.append(paragraphText);
//                        }
//                        textBuilder.append("\n"); // 문단 구분
//                    }
//                } catch (Exception e) {
//                    log.debug("문단 {} 처리 중 오류, 건너뜀: {}", i, e.getMessage());
//                }
//            }
//        } catch (Exception e) {
//            log.warn("HWP 섹션 텍스트 추출 중 오류 발생", e);
//        }
//    }
//    
//    /**
//     * 문단에서 텍스트 추출 (HWPLib 내장 기능 활용)
//     */
//    private String extractTextFromParagraph(Paragraph paragraph) {
//        try {
//            StringBuilder text = new StringBuilder();
//            
//            // HWPLib의 내장 toString() 메서드 활용
//            String paragraphText = paragraph.toString();
//            if (paragraphText != null && !paragraphText.trim().isEmpty()) {
//                // 클래스명이나 객체 정보가 아닌 실제 텍스트인지 확인
//                if (!paragraphText.startsWith("kr.dogfoot") && !paragraphText.contains("@")) {
//                    text.append(paragraphText);
//                }
//            }
//            
//            // toString()으로 추출되지 않는 경우 리플렉션 시도
//            if (text.length() == 0) {
//                try {
//                    // 가능한 필드명들 시도
//                    String[] possibleFields = {"text", "content", "textContent", "plainText"};
//                    
//                    for (String fieldName : possibleFields) {
//                        try {
//                            var field = paragraph.getClass().getDeclaredField(fieldName);
//                            field.setAccessible(true);
//                            Object fieldValue = field.get(paragraph);
//                            
//                            if (fieldValue != null) {
//                                String fieldText = fieldValue.toString();
//                                if (!fieldText.trim().isEmpty() && 
//                                    !fieldText.startsWith("kr.dogfoot") && 
//                                    !fieldText.contains("@")) {
//                                    text.append(fieldText);
//                                    break;
//                                }
//                            }
//                        } catch (Exception ignored) {
//                            // 필드가 없으면 다음 시도
//                        }
//                    }
//                } catch (Exception e) {
//                    log.debug("리플렉션을 통한 텍스트 추출 실패", e);
//                }
//            }
//            
//            return text.toString();
//            
//        } catch (Exception e) {
//            log.debug("문단 텍스트 추출 실패", e);
//            return "";
//        }
//    }
//    
    /**
     * ParaText에서 문자열 추출 (리플렉션을 통한 안전한 추출)
     */
    private String extractCharactersFromParaText(Object paraText) {
        try {
            // toString() 메서드를 사용한 단순 추출 시도
            String textStr = paraText.toString();
            
            // 의미있는 텍스트가 있는지 확인
            if (textStr != null && textStr.length() > 0 && !textStr.startsWith("kr.dogfoot")) {
                return textStr;
            }
            
            // 다른 방법으로 텍스트 추출 시도 (리플렉션)
            try {
                var field = paraText.getClass().getDeclaredField("text");
                field.setAccessible(true);
                Object textObj = field.get(paraText);
                if (textObj != null) {
                    return textObj.toString();
                }
            } catch (Exception e) {
                // 리플렉션 실패 시 무시
            }
            
        } catch (Exception e) {
            log.debug("문자 추출 실패", e);
        }
        
        return "";
    }

    /**
     * PDF에서 텍스트 추출
     */
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        StringBuilder result = new StringBuilder();
        
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            result.append(text);
            
            // 페이지 수 정보
            int pageCount = document.getNumberOfPages();
            
            // 이미지 개수 (간단한 추정)
            int estimatedImages = countImagesInPDF(document);
            
            log.info("PDF 추출 완료: {}페이지, 약 {}자, 이미지 약 {}개", 
                     pageCount, result.length(), estimatedImages);
            
        } catch (IOException e) {
            log.error("PDF 텍스트 추출 실패", e);
            throw new IOException("PDF 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        
        return result.toString();
    }

    /**
     * DOCX에서 텍스트 추출 (표 포함)
     */
    private String extractTextFromDOCX(MultipartFile file) throws IOException {
        StringBuilder result = new StringBuilder();
        
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {
            
            // 본문 텍스트 추출
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                result.append(paragraph.getText()).append("\n");
            }
            
            // 표 추출
            List<XWPFTable> tables = document.getTables();
            
            // 이미지 개수
            List<XWPFPictureData> pictures = document.getAllPictures();
            
            log.info("DOCX 추출 완료: 약 {}자, 표 {}개, 이미지 {}개", 
                     result.length(), tables.size(), pictures.size());
            
        } catch (IOException e) {
            log.error("DOCX 텍스트 추출 실패", e);
            throw new IOException("DOCX 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        
        return result.toString();
    }

    /**
     * 표를 텍스트로 변환
     */
    private String extractTableText(XWPFTable table) {
        StringBuilder tableText = new StringBuilder();
        
        List<XWPFTableRow> rows = table.getRows();
        
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            XWPFTableRow row = rows.get(rowIdx);
            List<XWPFTableCell> cells = row.getTableCells();
            
            // 첫 행은 헤더로 표시
            if (rowIdx == 0) {
                tableText.append("┌");
                for (int i = 0; i < cells.size(); i++) {
                    tableText.append("────────");
                    if (i < cells.size() - 1) {
                        tableText.append("┬");
                    }
                }
                tableText.append("┐\n");
            }
            
            // 셀 데이터
            tableText.append("│ ");
            for (XWPFTableCell cell : cells) {
                String cellText = cell.getText().trim();
                if (cellText.isEmpty()) {
                    cellText = "-";
                }
                // 셀 길이 제한 (너무 긴 경우)
                if (cellText.length() > 15) {
                    cellText = cellText.substring(0, 12) + "...";
                }
                tableText.append(String.format("%-15s", cellText)).append(" │ ");
            }
            tableText.append("\n");
            
            // 구분선
            if (rowIdx == 0) {
                tableText.append("├");
                for (int i = 0; i < cells.size(); i++) {
                    tableText.append("────────");
                    if (i < cells.size() - 1) {
                        tableText.append("┼");
                    }
                }
                tableText.append("┤\n");
            } else if (rowIdx < rows.size() - 1) {
                tableText.append("├");
                for (int i = 0; i < cells.size(); i++) {
                    tableText.append("────────");
                    if (i < cells.size() - 1) {
                        tableText.append("┼");
                    }
                }
                tableText.append("┤\n");
            }
        }
        
        // 마지막 구분선
        tableText.append("└");
        for (int i = 0; i < rows.get(0).getTableCells().size(); i++) {
            tableText.append("────────");
            if (i < rows.get(0).getTableCells().size() - 1) {
                tableText.append("┴");
            }
        }
        tableText.append("┘\n");
        
        return tableText.toString();
    }

    /**
     * PDF 내 이미지 개수 추정
     */
    private int countImagesInPDF(PDDocument document) {
        int imageCount = 0;
        
        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                var page = document.getPage(i);
                var resources = page.getResources();
                
                if (resources != null) {
                    var xObjectNames = resources.getXObjectNames();
                    if (xObjectNames != null) {
                        // Iterable을 직접 카운팅
                        for (var xObjectName : xObjectNames) {
                            try {
                                var xObject = resources.getXObject(xObjectName);
                                // 이미지인 경우에만 카운팅
                                if (xObject instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                                    imageCount++;
                                }
                            } catch (Exception e) {
                                // 개별 이미지 처리 실패는 무시
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("이미지 개수 세기 실패", e);
        }
        
        return imageCount;
    }
}
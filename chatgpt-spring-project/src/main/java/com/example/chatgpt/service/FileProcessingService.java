package com.example.chatgpt.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 파일 처리 서비스 (이미지/표 추출 개선 버전)
 */
@Service
@Slf4j
public class FileProcessingService {

    /**
     * 파일 형식 검증
     */
    public boolean isValidFileFormat(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".pdf") || lowerFilename.endsWith(".docx");
    }

    /**
     * 파일에서 텍스트 추출 (이미지/표 정보 포함)
     */
    public String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        
        if (filename == null) {
            throw new IOException("파일 이름이 없습니다.");
        }

        if (filename.toLowerCase().endsWith(".pdf")) {
            return extractTextFromPDF(file);
        } else if (filename.toLowerCase().endsWith(".docx")) {
            return extractTextFromDOCX(file);
        } else {
            throw new IOException("지원하지 않는 파일 형식입니다: " + filename);
        }
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
//            result.append("\n\n[문서 정보: ").append(pageCount).append("페이지]\n");
            
            // 이미지 개수 (간단한 추정)
            int estimatedImages = countImagesInPDF(document);
//            if (estimatedImages > 0) {
//                result.append("[이미지 ").append(estimatedImages).append("개 포함됨 - 텍스트로 변환 불가]\n");
//            }
            
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
//            if (!tables.isEmpty()) {
//                result.append("\n\n========== 표 데이터 ==========\n\n");
//                
//                for (int tableIdx = 0; tableIdx < tables.size(); tableIdx++) {
//                    XWPFTable table = tables.get(tableIdx);
//                    result.append(String.format("[표 %d]\n", tableIdx + 1));
//                    result.append(extractTableText(table));
//                    result.append("\n");
//                }
//            }
            
            // 이미지 개수
            List<XWPFPictureData> pictures = document.getAllPictures();
//            if (!pictures.isEmpty()) {
//                result.append(String.format("\n[이미지 %d개 포함됨 - 텍스트로 변환 불가]\n", pictures.size()));
//            }
            
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
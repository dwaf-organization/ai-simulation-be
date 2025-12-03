package com.example.chatgpt.service;

import com.example.chatgpt.dto.DecisionVariableDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 엑셀 파일에서 의사결정 변수를 로드하는 서비스
 */
@Service
@Slf4j
public class ExcelLoaderService {

    private static final String EXCEL_FILE_PATH = "data/decision_variables.xlsx";
    
    // Stage별 변수 저장
    private Map<Integer, List<DecisionVariableDto>> stageVariables = new HashMap<>();
    
    // 대분류별 그룹화 (Stage별)
    private Map<Integer, Map<String, List<DecisionVariableDto>>> stageGroupedByMajorCategory = new HashMap<>();

    /**
     * 애플리케이션 시작 시 엑셀 로드
     */
    @PostConstruct
    public void init() {
        try {
            loadExcel();
            log.info("엑셀 파일 로드 완료: {}", EXCEL_FILE_PATH);
            log.info("로드된 Stage 수: {}", stageVariables.size());
            stageVariables.forEach((stage, vars) -> 
                log.info("  Stage {}: {} 개 변수", stage, vars.size())
            );
        } catch (Exception e) {
            log.error("엑셀 파일 로드 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 엑셀 파일 로드
     */
    private void loadExcel() throws Exception {
        ClassPathResource resource = new ClassPathResource(EXCEL_FILE_PATH);
        
        try (InputStream is = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            // 모든 시트를 순회하며 Stage 데이터 로드
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                // Stage1, Stage2... 형식 확인
                if (sheetName.startsWith("Stage")) {
                    try {
                        int stageNumber = Integer.parseInt(sheetName.replace("Stage", ""));
                        List<DecisionVariableDto> variables = parseSheet(sheet);
                        stageVariables.put(stageNumber, variables);
                        
                        // 대분류별 그룹화
                        Map<String, List<DecisionVariableDto>> grouped = variables.stream()
                            .collect(Collectors.groupingBy(DecisionVariableDto::getMajorCategory));
                        stageGroupedByMajorCategory.put(stageNumber, grouped);
                        
                        log.info("Stage {} 로드 완료: {} 개 변수, {} 개 대분류", 
                            stageNumber, variables.size(), grouped.size());
                    } catch (NumberFormatException e) {
                        log.warn("Stage 번호 파싱 실패: {}", sheetName);
                    }
                }
            }
        }
    }

    /**
     * 시트 파싱
     */
    private List<DecisionVariableDto> parseSheet(Sheet sheet) {
        List<DecisionVariableDto> variables = new ArrayList<>();
        
        // 첫 행은 헤더이므로 스킵
        boolean isFirstRow = true;
        
        for (Row row : sheet) {
            if (isFirstRow) {
                isFirstRow = false;
                continue;
            }
            
            // 빈 행 스킵
            if (isRowEmpty(row)) {
                continue;
            }
            
            DecisionVariableDto dto = DecisionVariableDto.builder()
                .code(getCellValue(row.getCell(0)))
                .majorCategory(getCellValue(row.getCell(1)))
                .minorCategory(getCellValue(row.getCell(2)))
                .variableName(getCellValue(row.getCell(3)))
                .salesImpact(getCellValue(row.getCell(4)))
                .budgetRange(getCellValue(row.getCell(5)))
                .impactKpi(getCellValue(row.getCell(6)))
                .remarks(getCellValue(row.getCell(7)))
                .build();
            
            variables.add(dto);
        }
        
        return variables;
    }

    /**
     * 셀 값 추출
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * 빈 행 체크
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 특정 Stage의 모든 변수 조회
     */
    public List<DecisionVariableDto> getVariablesByStage(int stage) {
        return stageVariables.getOrDefault(stage, new ArrayList<>());
    }

    /**
     * 특정 Stage의 대분류별 변수 조회
     */
    public Map<String, List<DecisionVariableDto>> getGroupedVariablesByStage(int stage) {
        return stageGroupedByMajorCategory.getOrDefault(stage, new HashMap<>());
    }

    /**
     * 특정 Stage의 대분류 목록 조회
     */
    public Set<String> getMajorCategoriesByStage(int stage) {
        Map<String, List<DecisionVariableDto>> grouped = stageGroupedByMajorCategory.get(stage);
        return grouped != null ? grouped.keySet() : new HashSet<>();
    }

    /**
     * 특정 Stage, 대분류에서 랜덤으로 변수 1개 선택
     */
    public DecisionVariableDto getRandomVariableByStageAndCategory(int stage, String majorCategory) {
        Map<String, List<DecisionVariableDto>> grouped = stageGroupedByMajorCategory.get(stage);
        if (grouped == null) {
            return null;
        }
        
        List<DecisionVariableDto> variables = grouped.get(majorCategory);
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        return variables.get(random.nextInt(variables.size()));
    }

    /**
     * 사용 가능한 Stage 목록
     */
    public Set<Integer> getAvailableStages() {
        return stageVariables.keySet();
    }
}
package com.example.pdfreader.service;

import com.example.pdfreader.config.PdfConfig;
import com.example.pdfreader.entity.OdpBodyField;
import com.example.pdfreader.repository.OdpBodyFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfTableService {

    private final PdfConfig pdfConfig;
    private final OdpBodyFieldRepository odpBodyFieldRepository;

    @Transactional
    public void processAndSaveTables() {
        List<OdpBodyField> allFields = extractTablesFromPdf();
        
        if (allFields.isEmpty()) {
            log.warn("No table data found in the specified pages.");
            return;
        }

        printTableToConsole(allFields);
        saveToDatabase(allFields);
    }

    private List<OdpBodyField> extractTablesFromPdf() {
        List<OdpBodyField> allFields = new ArrayList<>();
        File pdfFile = new File(pdfConfig.getFilePath());

        if (!pdfFile.exists()) {
            log.error("PDF file not found: {}", pdfConfig.getFilePath());
            throw new RuntimeException("PDF file not found: " + pdfConfig.getFilePath());
        }

        try (PDDocument document = Loader.loadPDF(pdfFile, pdfConfig.getPassword())) {
            log.info("Successfully opened password-protected PDF: {}", pdfConfig.getFilePath());
            
            List<Integer> pageNumbers = pdfConfig.getPageNumbers();
            log.info("Processing pages: {}", pageNumbers);

            ObjectExtractor extractor = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();

            for (Integer pageNumber : pageNumbers) {
                if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
                    log.warn("Page {} is out of range. Total pages: {}", pageNumber, document.getNumberOfPages());
                    continue;
                }

                log.info("Extracting tables from page {}", pageNumber);
                Page page = extractor.extract(pageNumber);
                List<Table> tables = algorithm.extract(page);

                for (Table table : tables) {
                    List<OdpBodyField> pageFields = parseTable(table, pageNumber);
                    allFields.addAll(pageFields);
                }
            }

        } catch (IOException e) {
            log.error("Failed to read PDF file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read PDF file", e);
        }

        return allFields;
    }

    private List<OdpBodyField> parseTable(Table table, int pageNumber) {
        List<OdpBodyField> fields = new ArrayList<>();
        List<List<RectangularTextContainer>> rows = table.getRows();

        boolean isHeaderSkipped = false;

        for (List<RectangularTextContainer> row : rows) {
            if (row.size() < 4) {
                continue;
            }

            String col1 = getCellText(row, 0);
            String col2 = getCellText(row, 1);
            String col3 = getCellText(row, 2);
            String col4 = getCellText(row, 3);

            // 跳过表头行
            if (!isHeaderSkipped && isHeaderRow(col1, col2, col3, col4)) {
                log.debug("Skipping header row on page {}", pageNumber);
                isHeaderSkipped = true;
                continue;
            }

            // 跳过空行
            if (col1.isBlank() && col2.isBlank() && col3.isBlank() && col4.isBlank()) {
                continue;
            }

            try {
                Integer fieldNumber = null;
                if (!col1.isBlank()) {
                    // 处理可能的数字格式问题
                    String cleanNumber = col1.replaceAll("[^0-9]", "");
                    if (!cleanNumber.isEmpty()) {
                        fieldNumber = Integer.parseInt(cleanNumber);
                    }
                }

                OdpBodyField field = OdpBodyField.builder()
                        .fieldNumber(fieldNumber)
                        .fieldName(truncate(col2, 80))
                        .dataType(truncate(col3, 20))
                        .description(truncate(col4, 1000))
                        .build();

                fields.add(field);
            } catch (NumberFormatException e) {
                log.warn("Skipping row with invalid number format: {}", col1);
            }
        }

        log.info("Extracted {} records from page {}", fields.size(), pageNumber);
        return fields;
    }

    private String getCellText(List<RectangularTextContainer> row, int index) {
        if (index < row.size()) {
            String text = row.get(index).getText();
            return text != null ? text.trim().replaceAll("\\s+", " ") : "";
        }
        return "";
    }

    private boolean isHeaderRow(String col1, String col2, String col3, String col4) {
        return "#".equalsIgnoreCase(col1.trim()) 
                || "Field Name".equalsIgnoreCase(col2.trim())
                || "Data Type".equalsIgnoreCase(col3.trim())
                || "Description".equalsIgnoreCase(col4.trim());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private void printTableToConsole(List<OdpBodyField> fields) {
        log.info("========================================================================================================");
        log.info("                                    PDF TABLE CONTENTS                                                   ");
        log.info("========================================================================================================");
        
        String headerFormat = "| %-5s | %-30s | %-15s | %-50s |";
        String separator = "+" + "-".repeat(7) + "+" + "-".repeat(32) + "+" + "-".repeat(17) + "+" + "-".repeat(52) + "+";
        
        System.out.println(separator);
        System.out.printf(headerFormat + "%n", "#", "Field Name", "Data Type", "Description");
        System.out.println(separator);
        
        for (OdpBodyField field : fields) {
            System.out.println(field.toString());
        }
        
        System.out.println(separator);
        log.info("Total records: {}", fields.size());
    }

    private void saveToDatabase(List<OdpBodyField> fields) {
        log.info("Saving {} records to database...", fields.size());
        
        try {
            // 可选：清除现有数据
            // odpBodyFieldRepository.deleteAllRecords();
            
            List<OdpBodyField> savedFields = odpBodyFieldRepository.saveAll(fields);
            log.info("Successfully saved {} records to ODP_BODY_FIELDS table", savedFields.size());
        } catch (Exception e) {
            log.error("Failed to save records to database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save records to database", e);
        }
    }
}
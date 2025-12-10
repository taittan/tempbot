package com.example.pdfreader.runner;

import com.example.pdfreader.service.PdfTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfReaderRunner implements CommandLineRunner {

    private final PdfTableService pdfTableService;

    @Override
    public void run(String... args) {
        log.info("Starting PDF Table Reader...");
        
        try {
            pdfTableService.processAndSaveTables();
            log.info("PDF processing completed successfully!");
        } catch (Exception e) {
            log.error("Failed to process PDF: {}", e.getMessage(), e);
        }
    }
}
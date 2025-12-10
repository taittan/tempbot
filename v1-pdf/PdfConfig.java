package com.example.pdfreader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "pdf")
public class PdfConfig {

    private String filePath;
    private String password;
    private String pages;

    /**
     * 解析页数配置，支持以下格式：
     * - 单页: "5"
     * - 多页: "5,6,7"
     * - 范围: "5-7"
     * - 混合: "1,3-5,8"
     */
    public List<Integer> getPageNumbers() {
        List<Integer> pageNumbers = new ArrayList<>();
        
        if (pages == null || pages.isBlank()) {
            return pageNumbers;
        }

        String[] parts = pages.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                // 处理范围格式，如 "5-7"
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end; i++) {
                    pageNumbers.add(i);
                }
            } else {
                // 单个页码
                pageNumbers.add(Integer.parseInt(part));
            }
        }

        return pageNumbers;
    }
}
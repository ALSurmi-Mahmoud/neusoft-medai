package com.team.medaibackend.service;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Service for generating DOCX documents using Apache POI
 */
@Service
public class DocxExportService {

    /**
     * Generate DOCX from structured content
     *
     * @param title Document title
     * @param sections Map of section titles to content
     * @return DOCX as byte array
     * @throws IOException if generation fails
     */
    public byte[] generateDocx(String title, Map<String, String> sections) throws IOException {
        XWPFDocument document = new XWPFDocument();

        try {
            // Add title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.addBreak();
            titleRun.addBreak();

            // Add sections
            for (Map.Entry<String, String> section : sections.entrySet()) {
                // Section title
                XWPFParagraph sectionTitlePara = document.createParagraph();
                XWPFRun sectionTitleRun = sectionTitlePara.createRun();
                sectionTitleRun.setText(section.getKey());
                sectionTitleRun.setBold(true);
                sectionTitleRun.setFontSize(14);
                sectionTitleRun.addBreak();

                // Section content
                XWPFParagraph contentPara = document.createParagraph();
                XWPFRun contentRun = contentPara.createRun();
                contentRun.setText(section.getValue());
                contentRun.setFontSize(12);
                contentRun.addBreak();
                contentRun.addBreak();
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();

        } finally {
            document.close();
        }
    }

    /**
     * Generate DOCX from HTML-like template (simplified)
     *
     * @param htmlContent HTML content (will extract text)
     * @return DOCX as byte array
     * @throws IOException if generation fails
     */
    public byte[] generateDocxFromHtml(String htmlContent) throws IOException {
        XWPFDocument document = new XWPFDocument();

        try {
            // Strip HTML tags (simple approach)
            String plainText = htmlContent.replaceAll("<[^>]+>", "");
            plainText = plainText.replace("&nbsp;", " ");
            plainText = plainText.replace("&amp;", "&");
            plainText = plainText.replace("&lt;", "<");
            plainText = plainText.replace("&gt;", ">");

            // Split by double newlines to create paragraphs
            String[] paragraphs = plainText.split("\n\n");

            for (String para : paragraphs) {
                if (para.trim().isEmpty()) continue;

                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(para.trim());
                run.setFontSize(12);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();

        } finally {
            document.close();
        }
    }

    /**
     * Add table to document
     *
     * @param document DOCX document
     * @param data Table data (rows x columns)
     */
    public void addTable(XWPFDocument document, String[][] data) {
        if (data == null || data.length == 0) return;

        XWPFTable table = document.createTable(data.length, data[0].length);

        for (int row = 0; row < data.length; row++) {
            XWPFTableRow tableRow = table.getRow(row);
            for (int col = 0; col < data[row].length; col++) {
                tableRow.getCell(col).setText(data[row][col]);
            }
        }
    }

    /**
     * Replace placeholders in DOCX template
     * Note: This is a simplified version. For complex templates,
     * consider using Apache POI's XWPFTemplate or docx4j
     *
     * @param template Template content
     * @param variables Replacement variables
     * @return Content with replaced variables
     */
    public String replacePlaceholders(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }
}
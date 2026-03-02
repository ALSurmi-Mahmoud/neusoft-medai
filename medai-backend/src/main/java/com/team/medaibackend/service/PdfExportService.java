package com.team.medaibackend.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating PDF documents from HTML templates
 * Uses Flying Saucer + OpenPDF for HTML/CSS to PDF conversion
 */
@Service
public class PdfExportService {

    /**
     * Generate PDF from HTML content
     *
     * @param htmlContent HTML content with CSS
     * @return PDF as byte array
     * @throws Exception if PDF generation fails
     */
    public byte[] generatePdfFromHtml(String htmlContent) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Parse HTML to Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))
            );

            // Render to PDF
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(document, null);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                // Log but don't fail
            }
        }
    }

    /**
     * Replace placeholders in HTML template with actual values
     *
     * @param template HTML template with {variable} placeholders
     * @param variables Map of variable names to values
     * @return HTML with replaced values
     */
    public String replacePlaceholders(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";

            // Escape HTML special characters in value
            value = escapeHtml(value);

            result = result.replace(placeholder, value);
        }

        // Remove any remaining placeholders
        result = result.replaceAll("\\{[^}]+\\}", "");

        return result;
    }

    /**
     * Wrap HTML content with proper DOCTYPE and structure
     *
     * @param content HTML content
     * @return Complete HTML document
     */
    public String wrapHtmlDocument(String content) {
        return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }\n" +
                "        @page { size: A4; margin: 2cm; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                content +
                "\n</body>\n</html>";
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br/>");
    }

    /**
     * Build HTML table from map data
     *
     * @param data Map of label -> value pairs
     * @return HTML table string
     */
    public String buildHtmlTable(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        StringBuilder table = new StringBuilder();
        table.append("<table style=\"width:100%; border-collapse:collapse; margin:10px 0;\">");

        for (Map.Entry<String, String> entry : data.entrySet()) {
            table.append("<tr>");
            table.append("<td style=\"padding:8px; border:1px solid #ddd; font-weight:bold; width:200px; background-color:#f8f9fa;\">");
            table.append(escapeHtml(entry.getKey()));
            table.append("</td>");
            table.append("<td style=\"padding:8px; border:1px solid #ddd;\">");
            table.append(escapeHtml(entry.getValue()));
            table.append("</td>");
            table.append("</tr>");
        }

        table.append("</table>");
        return table.toString();
    }

    /**
     * Format vitals data as HTML table
     *
     * @param vitals Map of vital sign measurements
     * @return HTML table
     */
    public String formatVitalsTable(Map<String, Object> vitals) {
        if (vitals == null || vitals.isEmpty()) {
            return "<p>No vital signs recorded.</p>";
        }

        Map<String, String> vitalsString = new HashMap<>();

        // Common vital signs
        if (vitals.containsKey("temperature")) {
            vitalsString.put("Temperature", vitals.get("temperature") + " °F");
        }
        if (vitals.containsKey("blood_pressure")) {
            vitalsString.put("Blood Pressure", vitals.get("blood_pressure").toString() + " mmHg");
        }
        if (vitals.containsKey("heart_rate")) {
            vitalsString.put("Heart Rate", vitals.get("heart_rate") + " bpm");
        }
        if (vitals.containsKey("respiratory_rate")) {
            vitalsString.put("Respiratory Rate", vitals.get("respiratory_rate") + " /min");
        }
        if (vitals.containsKey("oxygen_saturation")) {
            vitalsString.put("Oxygen Saturation", vitals.get("oxygen_saturation") + " %");
        }
        if (vitals.containsKey("weight")) {
            vitalsString.put("Weight", vitals.get("weight") + " kg");
        }
        if (vitals.containsKey("height")) {
            vitalsString.put("Height", vitals.get("height") + " cm");
        }

        return buildHtmlTable(vitalsString);
    }
}
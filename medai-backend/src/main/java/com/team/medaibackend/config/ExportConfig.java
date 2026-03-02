package com.team.medaibackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "export")
public class ExportConfig {

    private String hospitalName = "Medical Imaging Center";
    private String logoPath;
    private Pdf pdf = new Pdf();
    private Docx docx = new Docx();

    public static class Pdf {
        private String defaultPageSize = "A4";
        private String defaultFontFamily = "Arial";
        private Integer defaultFontSize = 12;
        private Integer defaultMargin = 20;

        public String getDefaultPageSize() { return defaultPageSize; }
        public void setDefaultPageSize(String defaultPageSize) { this.defaultPageSize = defaultPageSize; }

        public String getDefaultFontFamily() { return defaultFontFamily; }
        public void setDefaultFontFamily(String defaultFontFamily) { this.defaultFontFamily = defaultFontFamily; }

        public Integer getDefaultFontSize() { return defaultFontSize; }
        public void setDefaultFontSize(Integer defaultFontSize) { this.defaultFontSize = defaultFontSize; }

        public Integer getDefaultMargin() { return defaultMargin; }
        public void setDefaultMargin(Integer defaultMargin) { this.defaultMargin = defaultMargin; }
    }

    public static class Docx {
        private String defaultFontFamily = "Arial";
        private Integer defaultFontSize = 12;

        public String getDefaultFontFamily() { return defaultFontFamily; }
        public void setDefaultFontFamily(String defaultFontFamily) { this.defaultFontFamily = defaultFontFamily; }

        public Integer getDefaultFontSize() { return defaultFontSize; }
        public void setDefaultFontSize(Integer defaultFontSize) { this.defaultFontSize = defaultFontSize; }
    }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public Pdf getPdf() { return pdf; }
    public void setPdf(Pdf pdf) { this.pdf = pdf; }

    public Docx getDocx() { return docx; }
    public void setDocx(Docx docx) { this.docx = docx; }
}
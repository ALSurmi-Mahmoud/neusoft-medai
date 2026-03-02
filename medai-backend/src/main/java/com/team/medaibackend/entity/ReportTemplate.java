package com.team.medaibackend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "report_templates")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String category; // radiology, clinical_note, treatment_plan, patient_summary

    @Column(columnDefinition = "TEXT")
    private String description;

    // Template Content
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "header_html", columnDefinition = "TEXT")
    private String headerHtml;

    @Column(name = "footer_html", columnDefinition = "TEXT")
    private String footerHtml;

    // Variables (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> variables;

    // Styling Options
    @Column(name = "page_size", length = 20)
    private String pageSize = "A4"; // A4, Letter, Legal

    @Column(name = "font_family", length = 50)
    private String fontFamily = "Arial";

    @Column(name = "font_size")
    private Integer fontSize = 12;

    @Column(name = "margin_top")
    private Integer marginTop = 20;

    @Column(name = "margin_bottom")
    private Integer marginBottom = 20;

    @Column(name = "margin_left")
    private Integer marginLeft = 20;

    @Column(name = "margin_right")
    private Integer marginRight = 20;

    // Metadata
    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getHeaderHtml() { return headerHtml; }
    public void setHeaderHtml(String headerHtml) { this.headerHtml = headerHtml; }

    public String getFooterHtml() { return footerHtml; }
    public void setFooterHtml(String footerHtml) { this.footerHtml = footerHtml; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }

    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

    public Integer getMarginTop() { return marginTop; }
    public void setMarginTop(Integer marginTop) { this.marginTop = marginTop; }

    public Integer getMarginBottom() { return marginBottom; }
    public void setMarginBottom(Integer marginBottom) { this.marginBottom = marginBottom; }

    public Integer getMarginLeft() { return marginLeft; }
    public void setMarginLeft(Integer marginLeft) { this.marginLeft = marginLeft; }

    public Integer getMarginRight() { return marginRight; }
    public void setMarginRight(Integer marginRight) { this.marginRight = marginRight; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
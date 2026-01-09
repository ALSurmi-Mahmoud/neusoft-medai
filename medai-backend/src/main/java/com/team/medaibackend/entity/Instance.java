package com.team.medaibackend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "instances")
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_uid", nullable = false, unique = true, length = 128)
    private String instanceUid;  // DICOM SOPInstanceUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Column(name = "instance_number")
    private Integer instanceNumber;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column
    private Integer rows;

    @Column
    private Integer columns;

    @Column(name = "pixel_spacing", length = 100)
    private String pixelSpacing;

    @Column(name = "slice_thickness", precision = 10, scale = 4)
    private BigDecimal sliceThickness;

    @Column(name = "slice_location", precision = 10, scale = 4)
    private BigDecimal sliceLocation;

    @Column(name = "image_orientation", length = 200)
    private String imageOrientation;

    @Column(name = "image_position", length = 200)
    private String imagePosition;

    @Column(name = "transfer_syntax_uid", length = 128)
    private String transferSyntaxUid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstanceUid() { return instanceUid; }
    public void setInstanceUid(String instanceUid) { this.instanceUid = instanceUid; }

    public Series getSeries() { return series; }
    public void setSeries(Series series) { this.series = series; }

    public Integer getInstanceNumber() { return instanceNumber; }
    public void setInstanceNumber(Integer instanceNumber) { this.instanceNumber = instanceNumber; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getColumns() { return columns; }
    public void setColumns(Integer columns) { this.columns = columns; }

    public String getPixelSpacing() { return pixelSpacing; }
    public void setPixelSpacing(String pixelSpacing) { this.pixelSpacing = pixelSpacing; }

    public BigDecimal getSliceThickness() { return sliceThickness; }
    public void setSliceThickness(BigDecimal sliceThickness) { this.sliceThickness = sliceThickness; }

    public BigDecimal getSliceLocation() { return sliceLocation; }
    public void setSliceLocation(BigDecimal sliceLocation) { this.sliceLocation = sliceLocation; }

    public String getImageOrientation() { return imageOrientation; }
    public void setImageOrientation(String imageOrientation) { this.imageOrientation = imageOrientation; }

    public String getImagePosition() { return imagePosition; }
    public void setImagePosition(String imagePosition) { this.imagePosition = imagePosition; }

    public String getTransferSyntaxUid() { return transferSyntaxUid; }
    public void setTransferSyntaxUid(String transferSyntaxUid) { this.transferSyntaxUid = transferSyntaxUid; }

    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
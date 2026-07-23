package com.nattechnologies.trastcms.domain.media;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_asset")
public class MediaAsset {
    @Id @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 255)
    private String filename;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(length = 255)
    private String title;
    @Column(name = "alt_text", length = 500)
    private String altText;
    @Column(length = 1000)
    private String caption;
    @Lob
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaKind kind = MediaKind.OTHER;
    @Column(length = 160)
    private String folder = "General";
    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    private Integer width;
    private Integer height;
    @Column(name = "duration_seconds")
    private Double durationSeconds;
    @Column(name = "storage_path", nullable = false, length = 600)
    private String storagePath;
    @Column(name = "public_url", nullable = false, length = 600)
    private String publicUrl;
    @Column(name = "uploaded_by", nullable = false, length = 190)
    private String uploadedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (title == null || title.isBlank()) title = originalFilename;
        if (kind == null) kind = MediaKind.fromContentType(contentType);
        if (folder == null || folder.isBlank()) folder = "General";
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MediaKind getKind() { return kind; }
    public void setKind(MediaKind kind) { this.kind = kind; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

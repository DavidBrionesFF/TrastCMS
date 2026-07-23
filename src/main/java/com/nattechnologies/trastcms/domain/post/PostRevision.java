package com.nattechnologies.trastcms.domain.post;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_revision")
public class PostRevision {
    @Id @Column(length = 36)
    private String id;
    @Column(name = "post_id", nullable = false, length = 36)
    private String postId;
    @Column(name = "revision_number", nullable = false)
    private long revisionNumber;
    @Column(nullable = false, length = 250)
    private String title;
    @Column(length = 600)
    private String excerpt;
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(name = "editor_mode", nullable = false, length = 30)
    private EditorMode editorMode;
    @Lob @Column(name = "builder_data")
    private String builderData;
    @Lob @Column(name = "custom_css")
    private String customCss;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostStatus status;
    @Column(name = "created_by", nullable = false, length = 190)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public long getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(long revisionNumber) { this.revisionNumber = revisionNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public EditorMode getEditorMode() { return editorMode; }
    public void setEditorMode(EditorMode editorMode) { this.editorMode = editorMode; }
    public String getBuilderData() { return builderData; }
    public void setBuilderData(String builderData) { this.builderData = builderData; }
    public String getCustomCss() { return customCss; }
    public void setCustomCss(String customCss) { this.customCss = customCss; }
    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}

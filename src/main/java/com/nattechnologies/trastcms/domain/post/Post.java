package com.nattechnologies.trastcms.domain.post;

import com.nattechnologies.trastcms.domain.category.Category;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post")
public class Post {
    @Id @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, unique = true, length = 280)
    private String slug;

    @Column(length = 600)
    private String excerpt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "editor_mode", nullable = false, length = 30)
    private EditorMode editorMode = EditorMode.RICH_TEXT;

    @Lob @Column(name = "builder_data")
    private String builderData;

    @Lob @Column(name = "custom_css")
    private String customCss;

    @Column(name = "seo_title", length = 250)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "page_template", length = 120)
    private String template = "default";

    @Column(name = "show_in_menu", nullable = false)
    private boolean showInMenu;
    @Column(name = "menu_order", nullable = false)
    private int menuOrder;
    @Column(name = "page_role", length = 40)
    private String pageRole;
    @Column(name = "theme_origin", length = 64)
    private String themeOrigin;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType = ContentType.POST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "featured_image_url", length = 500)
    private String featuredImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version @Column(nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (editorMode == null) {
            editorMode = contentType == ContentType.PAGE ? EditorMode.VISUAL_BUILDER : EditorMode.RICH_TEXT;
        }
        if (template == null || template.isBlank()) template = "default";
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public EditorMode getEditorMode() { return editorMode; }
    public void setEditorMode(EditorMode editorMode) { this.editorMode = editorMode; }
    public String getBuilderData() { return builderData; }
    public void setBuilderData(String builderData) { this.builderData = builderData; }
    public String getCustomCss() { return customCss; }
    public void setCustomCss(String customCss) { this.customCss = customCss; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public boolean isShowInMenu() { return showInMenu; }
    public void setShowInMenu(boolean showInMenu) { this.showInMenu = showInMenu; }
    public int getMenuOrder() { return menuOrder; }
    public void setMenuOrder(int menuOrder) { this.menuOrder = menuOrder; }
    public String getPageRole() { return pageRole; }
    public void setPageRole(String pageRole) { this.pageRole = pageRole; }
    public String getThemeOrigin() { return themeOrigin; }
    public void setThemeOrigin(String themeOrigin) { this.themeOrigin = themeOrigin; }
    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }
    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }
    public String getFeaturedImageUrl() { return featuredImageUrl; }
    public void setFeaturedImageUrl(String featuredImageUrl) { this.featuredImageUrl = featuredImageUrl; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public UserAccount getAuthor() { return author; }
    public void setAuthor(UserAccount author) { this.author = author; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

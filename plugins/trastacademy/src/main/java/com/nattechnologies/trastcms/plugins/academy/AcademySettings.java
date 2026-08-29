package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="academy_settings")
class AcademySettings {
    static String currentId(){ return com.nattechnologies.trastcms.multisite.SiteContext.siteId(); }
    @Id @Column(length=36) String id=currentId();
    @Column(name="catalog_title", nullable=false, length=220) String catalogTitle="Academy";
    @Column(name="catalog_description", length=1000) String catalogDescription="Aprenda con cursos prácticos, lecciones en video y evaluaciones.";
    @Column(name="default_currency", nullable=false, length=3) String defaultCurrency="USD";
    @Column(name="default_import_url", length=1000) String defaultImportUrl;
    @Column(name="allow_free_enrollment", nullable=false) boolean allowFreeEnrollment=true;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    @PrePersist void create(){createdAt=updatedAt=Instant.now();}
    @PreUpdate void update(){updatedAt=Instant.now();}
}

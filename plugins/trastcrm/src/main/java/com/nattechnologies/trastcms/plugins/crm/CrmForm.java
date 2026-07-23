package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="crm_form") class CrmForm {
 @Id @Column(length=36) String id; @Column(name="form_key",nullable=false,unique=true,length=120) String formKey; @Column(nullable=false,length=180) String name; @Column(length=1000) String description; @Column(name="success_message",nullable=false,length=500) String successMessage; @Column(name="notify_emails",length=1000) String notifyEmails; @Lob @Column(name="fields_json",nullable=false) String fieldsJson; @Lob @Column(name="settings_json") String settingsJson; @Column(nullable=false) boolean active=true; @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt; @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}

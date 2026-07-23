package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="crm_pipeline_stage") class CrmPipelineStage {
 @Id @Column(length=36) String id; @Column(nullable=false,length=120) String name; @Column(nullable=false) int position; @Column(nullable=false) int probability; @Column(nullable=false,length=20) String color="#6d4aff"; @Column(nullable=false) boolean closed; @Column(nullable=false) boolean won;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();}
}

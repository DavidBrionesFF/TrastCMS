package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.audit.AuditLog;
import com.nattechnologies.trastcms.domain.audit.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    @Transactional
    public void record(String actor, String action, String resourceType, String resourceId, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor == null ? "system" : actor);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        repository.save(log);
    }
}

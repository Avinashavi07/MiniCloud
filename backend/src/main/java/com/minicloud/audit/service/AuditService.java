package com.minicloud.audit.service;

import com.minicloud.audit.dto.AuditEventResponse;
import com.minicloud.audit.entity.AuditEvent;
import com.minicloud.audit.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String username, String action, String resourceType, String resourceId, String status, String message) {
        AuditEvent event = new AuditEvent();
        event.setUsername(username);
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setStatus(status);
        event.setMessage(message);
        auditEventRepository.save(event);
        log.info("Audit {} {} resource={} id={} status={}", username, action, resourceType, resourceId, status);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recent() {
        return auditEventRepository.findTop20ByOrderByTimestampDesc().stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> page(Pageable pageable) {
        return auditEventRepository.findAllByOrderByTimestampDesc(pageable).map(AuditEventResponse::from);
    }
}

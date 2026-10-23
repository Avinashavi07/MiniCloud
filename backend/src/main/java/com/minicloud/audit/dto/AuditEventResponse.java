package com.minicloud.audit.dto;

import com.minicloud.audit.entity.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String username,
        String action,
        String resourceType,
        String resourceId,
        Instant timestamp,
        String status,
        String message
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getUsername(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getTimestamp(),
                event.getStatus(),
                event.getMessage()
        );
    }
}

package com.minicloud.audit.repository;

import com.minicloud.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findTop20ByOrderByTimestampDesc();

    Page<AuditEvent> findAllByOrderByTimestampDesc(Pageable pageable);
}

package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.AuditLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    Optional<AuditLog> findById(UUID id);
    List<AuditLog> findAll();
    List<AuditLog> findAllOrderByTimestampDesc();
}

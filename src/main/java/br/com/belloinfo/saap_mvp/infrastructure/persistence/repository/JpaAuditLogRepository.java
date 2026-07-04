package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    Page<AuditLogEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}

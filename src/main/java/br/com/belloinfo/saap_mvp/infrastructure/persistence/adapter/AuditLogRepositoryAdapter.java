package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AuditLogEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaAuditLogRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaUserRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final JpaAuditLogRepository jpaAuditLogRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaAppointmentRepository jpaAppointmentRepository;
    private final CoreMapper mapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = mapper.toEntity(auditLog);

        if (auditLog.getUserId() != null) {
            entity.setUser(jpaUserRepository.getReferenceById(auditLog.getUserId()));
        }
        if (auditLog.getAppointmentId() != null) {
            entity.setAppointment(jpaAppointmentRepository.getReferenceById(auditLog.getAppointmentId()));
        }

        AuditLogEntity savedEntity = jpaAuditLogRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return jpaAuditLogRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<AuditLog> findAll() {
        return jpaAuditLogRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

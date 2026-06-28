package br.com.belloinfo.saap_mvp.application.service;

import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(String action, UUID resourceId, String resourceType, String userEmail, String ipAddress) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuário do log de auditoria não encontrado"));

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(user.getId())
                .action(action)
                .appointmentId("APPOINTMENT".equalsIgnoreCase(resourceType) ? resourceId : null)
                .recursoId(resourceId)
                .recursoTipo(resourceType)
                .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                .build();

        auditLogRepository.save(log);
    }
}

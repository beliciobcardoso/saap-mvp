package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar todos os logs de auditoria de forma ordenada (data decrescente)
 * e com resolução eficiente do e-mail do usuário responsável de forma a evitar N+1 queries.
 */
@Component
@RequiredArgsConstructor
public class ListAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public List<AuditLogWithEmail> execute() {
        List<AuditLog> logs = auditLogRepository.findAllOrderByTimestampDesc();

        // Mapeia todos os usuários cadastrados em memória por ID -> Email para evitar N+1 queries no banco
        Map<UUID, String> userIdToEmail = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (e1, e2) -> e1));

        return logs.stream()
                .map(log -> new AuditLogWithEmail(
                        log,
                        userIdToEmail.getOrDefault(log.getUserId(), "usuario.desativado@saap.com")
                ))
                .collect(Collectors.toList());
    }

    public record AuditLogWithEmail(AuditLog log, String userEmail) {}
}

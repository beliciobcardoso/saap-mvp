package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    public PageResult<AuditLogWithEmail> execute(int page, int size) {
        PageResult<AuditLog> logs = auditLogRepository.findAllOrderByTimestampDesc(page, size);

        // Busca apenas os usuários presente nos logs para evitar carregar a tabela inteira
        var userIds = logs.content().stream()
                .map(AuditLog::getUserId)
                .distinct()
                .toList();

        Map<UUID, String> userIdToEmail = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (e1, e2) -> e1));

        return new PageResult<>(
                logs.content().stream()
                        .map(log -> new AuditLogWithEmail(
                                log,
                                userIdToEmail.getOrDefault(log.getUserId(), "usuario.desativado@saap.com")
                        ))
                        .toList(),
                logs.page(),
                logs.size(),
                logs.totalElements(),
                logs.totalPages()
        );
    }

    public record AuditLogWithEmail(AuditLog log, String userEmail) {}
}

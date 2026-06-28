package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para representação de registros de auditoria na API REST do Administrador.
 */
public record AuditLogResponseDTO(
        UUID id,
        LocalDateTime timestamp,
        UUID userId,
        String userEmail,
        String action,
        UUID recursoId,
        String recursoTipo,
        String ipAddress
) {}

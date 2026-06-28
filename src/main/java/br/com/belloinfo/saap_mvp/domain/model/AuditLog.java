package br.com.belloinfo.saap_mvp.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private UUID id;
    private LocalDateTime timestamp;
    private UUID userId;
    private String action;
    private UUID appointmentId;
    private UUID recursoId;
    private String recursoTipo;
    private String ipAddress;
}

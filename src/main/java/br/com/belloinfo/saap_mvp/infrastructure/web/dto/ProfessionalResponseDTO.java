package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProfessionalResponseDTO(
    UUID id,
    String name,
    String email,
    String phone,
    String registrationNumber,
    ProfessionalRole role,
    UUID userId,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

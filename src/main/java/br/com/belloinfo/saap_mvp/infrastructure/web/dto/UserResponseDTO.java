package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO(
    UUID id,
    String email,
    UserRole role,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

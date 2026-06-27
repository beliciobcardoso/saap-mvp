package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResponseDTO(
    UUID id,
    String name,
    String description,
    int durationMinutes,
    BigDecimal price,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckInRequestDTO(
    @NotNull(message = "O nível de prioridade validado é obrigatório")
    PriorityLevel verifiedLevel,

    UUID verifiedBy,

    String notes
) {}

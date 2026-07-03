package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMedicalRecordEntryRequestDTO(
        @NotNull(message = "O agendamento é obrigatório")
        UUID appointmentId,
        @NotBlank(message = "A evolução clínica é obrigatória")
        String evolution
) {
}

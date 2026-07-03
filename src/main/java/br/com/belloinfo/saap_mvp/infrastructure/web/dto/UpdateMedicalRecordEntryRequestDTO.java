package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMedicalRecordEntryRequestDTO(
        @NotBlank(message = "A evolução clínica é obrigatória")
        String evolution
) {
}

package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalRecordEntryResponseDTO(
        UUID id,
        UUID medicalRecordId,
        UUID appointmentId,
        UUID professionalId,
        String evolution,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

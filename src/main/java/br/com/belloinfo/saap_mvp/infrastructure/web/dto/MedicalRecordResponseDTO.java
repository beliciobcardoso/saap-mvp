package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MedicalRecordResponseDTO(
        UUID id,
        UUID patientId,
        LocalDateTime createdAt,
        List<MedicalRecordEntryResponseDTO> entries
) {
}

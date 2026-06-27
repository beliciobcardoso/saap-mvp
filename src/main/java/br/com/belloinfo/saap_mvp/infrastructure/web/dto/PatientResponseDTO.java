package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponseDTO(
    UUID id,
    String name,
    String cpf,
    String susNumber,
    String email,
    String phone,
    LocalDate birthDate,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

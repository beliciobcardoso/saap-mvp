package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDTO(
    UUID id,
    PatientResponseDTO patient,
    ProfessionalResponseDTO professional,
    ServiceResponseDTO service,
    LocalDateTime dateTime,
    AppointmentStatus status,
    PaymentMethod paymentMethod,
    PriorityLevel priorityLevel,
    Long priorityScore,
    LocalDateTime priorityDeclaredAt,
    UUID priorityVerifiedBy,
    String priorityNotes,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

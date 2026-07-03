package br.com.belloinfo.saap_mvp.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordEntry {
    private UUID id;
    private UUID medicalRecordId;
    private UUID appointmentId;
    private UUID professionalId;
    private String evolution;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package br.com.belloinfo.saap_mvp.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {
    private UUID id;
    private UUID patientId;
    private LocalDateTime createdAt;
    private List<MedicalRecordEntry> entries;
}

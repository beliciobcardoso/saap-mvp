package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;

import java.util.Optional;
import java.util.UUID;

public interface MedicalRecordEntryRepository {
    MedicalRecordEntry save(MedicalRecordEntry entry);
    Optional<MedicalRecordEntry> findById(UUID id);
    Optional<MedicalRecordEntry> findByAppointmentId(UUID appointmentId);
}

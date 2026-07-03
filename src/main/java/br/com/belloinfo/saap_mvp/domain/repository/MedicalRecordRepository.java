package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;

import java.util.Optional;
import java.util.UUID;

public interface MedicalRecordRepository {
    MedicalRecord save(MedicalRecord medicalRecord);
    Optional<MedicalRecord> findById(UUID id);
    Optional<MedicalRecord> findByPatientId(UUID patientId);
}

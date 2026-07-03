package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaMedicalRecordRepository extends JpaRepository<MedicalRecordEntity, UUID> {
    Optional<MedicalRecordEntity> findByPatientId(UUID patientId);
}

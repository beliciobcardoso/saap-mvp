package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaMedicalRecordEntryRepository extends JpaRepository<MedicalRecordEntryEntity, UUID> {
    Optional<MedicalRecordEntryEntity> findByAppointmentId(UUID appointmentId);
}

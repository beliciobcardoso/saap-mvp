package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AppointmentEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntryEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.ProfessionalEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaMedicalRecordEntryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MedicalRecordEntryRepositoryAdapter implements MedicalRecordEntryRepository {

    private final JpaMedicalRecordEntryRepository jpaMedicalRecordEntryRepository;
    private final CoreMapper mapper;
    private final EntityManager entityManager;

    @Override
    public MedicalRecordEntry save(MedicalRecordEntry entry) {
        MedicalRecordEntryEntity entity = mapper.toEntity(entry);
        // Referências gerenciadas: stubs criados pelo mapper (só com id) são
        // tratados como transientes pelo Hibernate e quebram o persist.
        entity.setMedicalRecord(entityManager.getReference(MedicalRecordEntity.class, entry.getMedicalRecordId()));
        entity.setAppointment(entityManager.getReference(AppointmentEntity.class, entry.getAppointmentId()));
        entity.setProfessional(entityManager.getReference(ProfessionalEntity.class, entry.getProfessionalId()));
        MedicalRecordEntryEntity savedEntity = jpaMedicalRecordEntryRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MedicalRecordEntry> findById(UUID id) {
        return jpaMedicalRecordEntryRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MedicalRecordEntry> findByAppointmentId(UUID appointmentId) {
        return jpaMedicalRecordEntryRepository.findByAppointmentId(appointmentId).map(mapper::toDomain);
    }
}

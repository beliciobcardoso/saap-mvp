package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.PatientEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaMedicalRecordRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MedicalRecordRepositoryAdapter implements MedicalRecordRepository {

    private final JpaMedicalRecordRepository jpaMedicalRecordRepository;
    private final CoreMapper mapper;
    private final EntityManager entityManager;

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        MedicalRecordEntity entity = mapper.toEntity(medicalRecord);
        // Referência gerenciada: stub criado pelo mapper (só com id) é
        // tratado como transiente pelo Hibernate e quebra o persist.
        entity.setPatient(entityManager.getReference(PatientEntity.class, medicalRecord.getPatientId()));
        MedicalRecordEntity savedEntity = jpaMedicalRecordRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MedicalRecord> findById(UUID id) {
        return jpaMedicalRecordRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MedicalRecord> findByPatientId(UUID patientId) {
        return jpaMedicalRecordRepository.findByPatientId(patientId).map(mapper::toDomain);
    }
}

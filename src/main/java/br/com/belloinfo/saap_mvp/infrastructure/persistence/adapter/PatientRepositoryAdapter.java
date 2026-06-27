package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.PatientEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaPatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PatientRepositoryAdapter implements PatientRepository {

    private final JpaPatientRepository jpaPatientRepository;
    private final CoreMapper mapper;

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = mapper.toEntity(patient);
        PatientEntity savedEntity = jpaPatientRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return jpaPatientRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Patient> findByEmail(String email) {
        return jpaPatientRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<Patient> findAll() {
        return jpaPatientRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

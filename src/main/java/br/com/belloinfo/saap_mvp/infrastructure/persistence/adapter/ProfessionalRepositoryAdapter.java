package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.ProfessionalEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProfessionalRepositoryAdapter implements ProfessionalRepository {

    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final CoreMapper mapper;

    @Override
    public Professional save(Professional professional) {
        ProfessionalEntity entity = mapper.toEntity(professional);
        ProfessionalEntity savedEntity = jpaProfessionalRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Professional> findById(UUID id) {
        return jpaProfessionalRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Professional> findByRegistrationNumber(String registrationNumber) {
        return jpaProfessionalRepository.findByRegistrationNumber(registrationNumber).map(mapper::toDomain);
    }

    @Override
    public List<Professional> findAllActive() {
        // As long as JpaProfessionalRepository is annotated with @SQLRestriction("is_active = true")
        // findAll() will automatically return only active ones.
        return jpaProfessionalRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Professional> findAll() {
        // Standard findAll also returns active ones due to @SQLRestriction
        return jpaProfessionalRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

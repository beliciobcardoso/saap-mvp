package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.PaginationSupport;
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
    public Optional<Professional> findByIdWithLock(UUID id) {
        return jpaProfessionalRepository.findByIdWithLock(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Professional> findByRegistrationNumber(String registrationNumber) {
        return jpaProfessionalRepository.findByRegistrationNumber(registrationNumber).map(mapper::toDomain);
    }

    @Override
    public Optional<Professional> findByUserId(UUID userId) {
        return jpaProfessionalRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Professional> findActive(int page, int size) {
        // As long as JpaProfessionalRepository is annotated with @SQLRestriction("is_active = true")
        // findAll() will automatically return only active ones.
        return PaginationSupport.toPageResult(
                jpaProfessionalRepository.findAll(PaginationSupport.of(page, size)),
                mapper::toDomain
        );
    }

    @Override
    public List<Professional> findAll() {
        // Standard findAll also returns active ones due to @SQLRestriction
        return jpaProfessionalRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

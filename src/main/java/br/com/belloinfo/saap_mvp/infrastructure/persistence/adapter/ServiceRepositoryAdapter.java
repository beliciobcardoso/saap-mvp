package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.PaginationSupport;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.ServiceEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServiceRepository {

    private final JpaServiceRepository jpaServiceRepository;
    private final CoreMapper mapper;

    @Override
    public Service save(Service service) {
        ServiceEntity entity = mapper.toEntity(service);
        ServiceEntity savedEntity = jpaServiceRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Service> findById(UUID id) {
        return jpaServiceRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Service> findByName(String name) {
        return jpaServiceRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public PageResult<Service> findActive(int page, int size) {
        return PaginationSupport.toPageResult(
                jpaServiceRepository.findAll(PaginationSupport.of(page, size)),
                mapper::toDomain
        );
    }

    @Override
    public List<Service> findAll() {
        return jpaServiceRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

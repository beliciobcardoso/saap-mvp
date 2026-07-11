package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.PaginationSupport;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.UserEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final CoreMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity savedEntity = jpaUserRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaUserRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByIdIn(List<UUID> ids) {
        return jpaUserRepository.findByIdIn(ids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<User> findActive(int page, int size) {
        return PaginationSupport.toPageResult(
                jpaUserRepository.findByActiveTrue(PaginationSupport.of(page, size)),
                mapper::toDomain
        );
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Page<UserEntity> findByActiveTrue(Pageable pageable);
    List<UserEntity> findByIdIn(List<UUID> ids);
}

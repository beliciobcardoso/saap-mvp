package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, UUID> {
    Optional<ProfessionalEntity> findByRegistrationNumber(String registrationNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProfessionalEntity p WHERE p.id = :id")
    Optional<ProfessionalEntity> findByIdWithLock(@Param("id") UUID id);

    Optional<ProfessionalEntity> findByUserId(UUID userId);
}

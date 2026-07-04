package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.PatientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaPatientRepository extends JpaRepository<PatientEntity, UUID> {
    Optional<PatientEntity> findByEmail(String email);
    Optional<PatientEntity> findByCpf(String cpf);
    Optional<PatientEntity> findBySusNumber(String susNumber);
    Page<PatientEntity> findByActiveTrue(Pageable pageable);
}

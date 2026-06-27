package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.Professional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository {
    Professional save(Professional professional);
    Optional<Professional> findById(UUID id);
    Optional<Professional> findByRegistrationNumber(String registrationNumber);
    List<Professional> findAllActive();
    List<Professional> findAll();
}

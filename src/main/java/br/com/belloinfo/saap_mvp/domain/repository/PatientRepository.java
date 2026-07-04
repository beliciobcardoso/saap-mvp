package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.Patient;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByCpf(String cpf);
    PageResult<Patient> findActive(int page, int size);
}

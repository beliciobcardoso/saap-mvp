package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository {
    Service save(Service service);
    Optional<Service> findById(UUID id);
    Optional<Service> findByName(String name);
    List<Service> findAllActive();
    List<Service> findAll();
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class FindServiceByIdUseCase {
    private final ServiceRepository serviceRepository;

    public FindServiceByIdUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public Optional<Service> execute(UUID id) {
        return serviceRepository.findById(id);
    }
}

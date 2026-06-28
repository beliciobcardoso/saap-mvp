package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;

@org.springframework.stereotype.Service
public class CreateServiceUseCase {
    private final ServiceRepository serviceRepository;

    public CreateServiceUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public Service execute(Service service) {
        if (service.getName() != null && serviceRepository.findByName(service.getName()).isPresent()) {
            throw new IllegalArgumentException("Serviço com este nome já cadastrado");
        }
        service.activate();
        return serviceRepository.save(service);
    }
}

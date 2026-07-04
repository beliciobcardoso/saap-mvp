package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import java.util.UUID;

@org.springframework.stereotype.Service
public class DeactivateServiceUseCase {
    private final ServiceRepository serviceRepository;

    public DeactivateServiceUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public void execute(UUID id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException("Serviço não encontrado"));
        service.deactivate();
        serviceRepository.save(service);
    }
}

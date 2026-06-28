package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import java.util.UUID;

@org.springframework.stereotype.Service
public class UpdateServiceUseCase {
    private final ServiceRepository serviceRepository;

    public UpdateServiceUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public Service execute(UUID id, Service updated) {
        Service existing = serviceRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Serviço não encontrado"));

        if (updated.getName() != null && !updated.getName().equals(existing.getName())
            && serviceRepository.findByName(updated.getName()).isPresent()) {
            throw new IllegalArgumentException("Serviço com este nome já cadastrado");
        }

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setDurationMinutes(updated.getDurationMinutes());
        existing.setPrice(updated.getPrice());
        return serviceRepository.save(existing);
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import java.util.List;

@org.springframework.stereotype.Service
public class ListActiveServicesUseCase {
    private final ServiceRepository serviceRepository;

    public ListActiveServicesUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<Service> execute() {
        return serviceRepository.findAllActive();
    }
}

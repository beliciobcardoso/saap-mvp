package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;

@org.springframework.stereotype.Service
public class ListActiveServicesUseCase {
    private final ServiceRepository serviceRepository;

    public ListActiveServicesUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public PageResult<Service> execute(int page, int size) {
        return serviceRepository.findActive(page, size);
    }
}

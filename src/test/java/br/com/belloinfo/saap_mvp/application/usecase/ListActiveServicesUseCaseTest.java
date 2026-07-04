package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActiveServicesUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    private ListActiveServicesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActiveServicesUseCase(serviceRepository);
    }

    @Test
    @DisplayName("retorna lista de serviços ativos")
    void execute_withActiveServices_returnsList() {
        List<Service> activeServices = List.of(
                Service.builder().name("Consulta").active(true).build(),
                Service.builder().name("Exame").active(true).build()
        );
        when(serviceRepository.findAllActive()).thenReturn(activeServices);

        List<Service> result = useCase.execute();

        assertEquals(2, result.size());
        assertEquals(activeServices, result);
    }

    @Test
    @DisplayName("retorna lista vazia quando não há serviços ativos")
    void execute_withNoActiveServices_returnsEmptyList() {
        when(serviceRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Service> result = useCase.execute();

        assertTrue(result.isEmpty());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
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
    @DisplayName("retorna PageResult de serviços ativos")
    void execute_withActiveServices_returnsPageResult() {
        List<Service> activeServices = List.of(
                Service.builder().name("Consulta").active(true).build(),
                Service.builder().name("Exame").active(true).build()
        );
        PageResult<Service> expected = new PageResult<>(activeServices, 0, 20, 2, 1);
        when(serviceRepository.findActive(0, 20)).thenReturn(expected);

        PageResult<Service> result = useCase.execute(0, 20);

        assertEquals(2, result.content().size());
        assertEquals(activeServices, result.content());
    }

    @Test
    @DisplayName("retorna PageResult vazio quando não há serviços ativos")
    void execute_withNoActiveServices_returnsEmptyPageResult() {
        when(serviceRepository.findActive(0, 20)).thenReturn(new PageResult<>(Collections.emptyList(), 0, 20, 0, 0));

        PageResult<Service> result = useCase.execute(0, 20);

        assertTrue(result.content().isEmpty());
    }
}

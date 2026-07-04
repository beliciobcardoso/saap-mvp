package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindServiceByIdUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    private FindServiceByIdUseCase useCase;

    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new FindServiceByIdUseCase(serviceRepository);
    }

    @Test
    @DisplayName("retorna serviço quando encontrado pelo id")
    void execute_withExistingId_returnsService() {
        Service service = Service.builder().id(serviceId).name("Consulta").build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

        Optional<Service> result = useCase.execute(serviceId);

        assertTrue(result.isPresent());
        assertEquals(serviceId, result.get().getId());
    }

    @Test
    @DisplayName("retorna Optional vazio quando serviço não é encontrado")
    void execute_withNonExistentId_returnsEmptyOptional() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        Optional<Service> result = useCase.execute(serviceId);

        assertTrue(result.isEmpty());
    }
}

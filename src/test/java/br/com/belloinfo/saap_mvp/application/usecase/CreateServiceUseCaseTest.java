package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateServiceUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    private CreateServiceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateServiceUseCase(serviceRepository);
    }

    private Service newService(String name) {
        return Service.builder()
                .name(name)
                .description("Consulta de rotina")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150))
                .build();
    }

    @Test
    @DisplayName("cria serviço com sucesso quando nome não está cadastrado")
    void execute_withUniqueName_createsAndActivatesService() {
        Service service = newService("Consulta");
        when(serviceRepository.findByName("Consulta")).thenReturn(Optional.empty());
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service created = useCase.execute(service);

        assertTrue(created.isActive());
        verify(serviceRepository).save(service);
    }

    @Test
    @DisplayName("rejeita criação quando já existe serviço com o mesmo nome")
    void execute_withDuplicateName_throwsIllegalArgumentException() {
        Service service = newService("Consulta");
        when(serviceRepository.findByName("Consulta")).thenReturn(Optional.of(newService("Consulta")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(service));

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("cria serviço sem verificar duplicidade quando nome é nulo")
    void execute_withNullName_skipsDuplicateCheckAndCreates() {
        Service service = newService(null);
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service created = useCase.execute(service);

        assertTrue(created.isActive());
        verify(serviceRepository, never()).findByName(any());
        verify(serviceRepository).save(service);
    }
}

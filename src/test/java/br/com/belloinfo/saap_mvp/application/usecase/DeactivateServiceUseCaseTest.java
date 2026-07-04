package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeactivateServiceUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    private DeactivateServiceUseCase useCase;

    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeactivateServiceUseCase(serviceRepository);
    }

    @Test
    @DisplayName("desativa serviço ativo com sucesso")
    void execute_withActiveService_deactivatesAndSaves() {
        Service service = Service.builder().id(serviceId).name("Consulta").active(true).build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(serviceId);

        ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    @DisplayName("mantém serviço já inativo como inativo ao desativar novamente")
    void execute_withAlreadyInactiveService_staysDeactivatedAndSaves() {
        Service service = Service.builder().id(serviceId).name("Consulta").active(false).build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(serviceId);

        ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    @DisplayName("lança exceção quando serviço não é encontrado")
    void execute_withNonExistentService_throwsResourceNotFoundException() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(serviceId));

        verify(serviceRepository, never()).save(any());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateServiceUseCaseTest {

    @Mock
    private ServiceRepository serviceRepository;

    private UpdateServiceUseCase useCase;

    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdateServiceUseCase(serviceRepository);
    }

    private Service existingService() {
        return Service.builder()
                .id(serviceId)
                .name("Consulta")
                .description("Consulta padrão")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("atualiza serviço existente com sucesso")
    void execute_withExistingServiceAndUniqueName_updatesAndSaves() {
        Service existing = existingService();
        Service updated = Service.builder()
                .name("Consulta Especializada")
                .description("Nova descrição")
                .durationMinutes(45)
                .price(BigDecimal.valueOf(200))
                .build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(existing));
        when(serviceRepository.findByName("Consulta Especializada")).thenReturn(Optional.empty());
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service result = useCase.execute(serviceId, updated);

        assertEquals("Consulta Especializada", result.getName());
        assertEquals("Nova descrição", result.getDescription());
        assertEquals(45, result.getDurationMinutes());
        assertEquals(BigDecimal.valueOf(200), result.getPrice());
        verify(serviceRepository).save(existing);
    }

    @Test
    @DisplayName("lança exceção quando serviço não é encontrado")
    void execute_withNonExistentService_throwsResourceNotFoundException() {
        Service updated = Service.builder().name("Consulta").build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(serviceId, updated));

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita atualização quando novo nome já pertence a outro serviço")
    void execute_withNameAlreadyUsedByAnotherService_throwsIllegalArgumentException() {
        Service existing = existingService();
        Service updated = Service.builder().name("Outro Serviço").build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(existing));
        when(serviceRepository.findByName("Outro Serviço"))
                .thenReturn(Optional.of(Service.builder().id(UUID.randomUUID()).name("Outro Serviço").build()));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(serviceId, updated));

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("permite manter o mesmo nome sem checar duplicidade")
    void execute_withSameName_doesNotCheckDuplicateAndSaves() {
        Service existing = existingService();
        Service updated = Service.builder()
                .name("Consulta")
                .description("Descrição atualizada")
                .durationMinutes(60)
                .price(BigDecimal.valueOf(300))
                .build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service result = useCase.execute(serviceId, updated);

        assertEquals("Consulta", result.getName());
        verify(serviceRepository, never()).findByName(any());
        verify(serviceRepository).save(existing);
    }

    @Test
    @DisplayName("atualiza sem verificar duplicidade quando novo nome é nulo")
    void execute_withNullName_skipsDuplicateCheckAndSaves() {
        Service existing = existingService();
        Service updated = Service.builder()
                .name(null)
                .description("Descrição atualizada")
                .durationMinutes(60)
                .price(BigDecimal.valueOf(300))
                .build();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service result = useCase.execute(serviceId, updated);

        assertNull(result.getName());
        verify(serviceRepository, never()).findByName(any());
        verify(serviceRepository).save(existing);
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeactivateProfessionalUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    private DeactivateProfessionalUseCase useCase;

    private final UUID professionalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeactivateProfessionalUseCase(professionalRepository);
    }

    private Professional activeProfessional() {
        return Professional.builder()
                .id(professionalId)
                .name("Dra. Carla Mendes")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("desativa profissional ativo e persiste alteração")
    void execute_activeProfessional_deactivatesAndSaves() {
        Professional professional = activeProfessional();
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(professionalId);

        assertFalse(professional.isActive());
        verify(professionalRepository).save(professional);
    }

    @Test
    @DisplayName("desativa profissional já inativo sem erro, mantendo idempotência")
    void execute_alreadyInactiveProfessional_staysInactiveAndSaves() {
        Professional professional = activeProfessional();
        professional.deactivate();
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(professionalId);

        assertFalse(professional.isActive());
        verify(professionalRepository).save(professional);
    }

    @Test
    @DisplayName("lança ResourceNotFoundException quando profissional não existe")
    void execute_nonExistingProfessional_throwsResourceNotFoundException() {
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(professionalId));

        verify(professionalRepository, never()).save(any());
    }
}

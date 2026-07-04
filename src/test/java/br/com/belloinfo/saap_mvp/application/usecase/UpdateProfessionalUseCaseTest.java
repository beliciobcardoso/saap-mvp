package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
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
class UpdateProfessionalUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    private UpdateProfessionalUseCase useCase;

    private final UUID professionalId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdateProfessionalUseCase(professionalRepository);
    }

    private Professional existingProfessional() {
        return Professional.builder()
                .id(professionalId)
                .name("Dr. João Lima")
                .email("joao.lima@example.com")
                .phone("11988887777")
                .registrationNumber("CRM-1000")
                .role(ProfessionalRole.PROFESSIONAL)
                .active(true)
                .build();
    }

    private Professional updatedData(String registrationNumber) {
        return Professional.builder()
                .name("Dr. João Lima Silva")
                .email("joao.silva@example.com")
                .phone("11977776666")
                .registrationNumber(registrationNumber)
                .role(ProfessionalRole.ASSISTANT)
                .userId(userId)
                .build();
    }

    @Test
    @DisplayName("atualiza dados do profissional existente e persiste alterações")
    void execute_existingProfessional_updatesAndSaves() {
        Professional existing = existingProfessional();
        Professional updated = updatedData("CRM-2000");
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(existing));
        when(professionalRepository.findByRegistrationNumber("CRM-2000")).thenReturn(Optional.empty());
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        Professional result = useCase.execute(professionalId, updated);

        assertEquals("Dr. João Lima Silva", result.getName());
        assertEquals("joao.silva@example.com", result.getEmail());
        assertEquals("11977776666", result.getPhone());
        assertEquals("CRM-2000", result.getRegistrationNumber());
        assertEquals(ProfessionalRole.ASSISTANT, result.getRole());
        assertEquals(userId, result.getUserId());
        verify(professionalRepository).save(existing);
    }

    @Test
    @DisplayName("mantém mesmo registro profissional sem checar duplicidade quando não houve alteração")
    void execute_sameRegistrationNumber_skipsDuplicateCheck() {
        Professional existing = existingProfessional();
        Professional updated = updatedData("CRM-1000");
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(existing));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        Professional result = useCase.execute(professionalId, updated);

        assertEquals("CRM-1000", result.getRegistrationNumber());
        verify(professionalRepository, never()).findByRegistrationNumber(any());
        verify(professionalRepository).save(existing);
    }

    @Test
    @DisplayName("permite atualização sem informar registro profissional, ignorando verificação de duplicidade")
    void execute_withoutRegistrationNumber_skipsDuplicateCheck() {
        Professional existing = existingProfessional();
        Professional updated = updatedData(null);
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(existing));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        Professional result = useCase.execute(professionalId, updated);

        assertNull(result.getRegistrationNumber());
        verify(professionalRepository, never()).findByRegistrationNumber(any());
        verify(professionalRepository).save(existing);
    }

    @Test
    @DisplayName("lança ResourceNotFoundException quando profissional não existe")
    void execute_nonExistingProfessional_throwsResourceNotFoundException() {
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(professionalId, updatedData("CRM-2000")));

        verify(professionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita atualização quando novo registro profissional já pertence a outro profissional")
    void execute_registrationNumberOwnedByAnother_throwsIllegalArgumentException() {
        Professional existing = existingProfessional();
        Professional anotherProfessional = Professional.builder()
                .id(UUID.randomUUID())
                .registrationNumber("CRM-2000")
                .build();
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(existing));
        when(professionalRepository.findByRegistrationNumber("CRM-2000")).thenReturn(Optional.of(anotherProfessional));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(professionalId, updatedData("CRM-2000")));

        verify(professionalRepository, never()).save(any());
    }
}

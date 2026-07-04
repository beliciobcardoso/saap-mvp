package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
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
class DeactivatePatientUseCaseTest {

    @Mock
    private PatientRepository patientRepository;

    private DeactivatePatientUseCase useCase;

    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeactivatePatientUseCase(patientRepository);
    }

    @Test
    @DisplayName("desativa paciente ativo e persiste a alteração")
    void execute_activePatient_deactivatesAndSaves() {
        Patient patient = Patient.builder().id(patientId).name("Maria Silva").active(true).build();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);

        useCase.execute(patientId);

        verify(patientRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    @DisplayName("mantém idempotência ao desativar paciente já inativo")
    void execute_alreadyInactivePatient_staysInactiveAndSaves() {
        Patient patient = Patient.builder().id(patientId).name("Maria Silva").active(false).build();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        useCase.execute(patientId);

        assertFalse(patient.isActive());
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("lança exceção quando paciente não é encontrado")
    void execute_patientNotFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(patientId));

        verify(patientRepository, never()).save(any());
    }
}

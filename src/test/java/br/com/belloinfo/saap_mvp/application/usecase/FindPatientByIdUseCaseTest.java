package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindPatientByIdUseCaseTest {

    @Mock
    private PatientRepository patientRepository;

    private FindPatientByIdUseCase useCase;

    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new FindPatientByIdUseCase(patientRepository);
    }

    @Test
    @DisplayName("retorna paciente presente quando encontrado pelo repositório")
    void execute_existingPatient_returnsOptionalWithPatient() {
        Patient patient = Patient.builder().id(patientId).name("Maria Silva").build();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        Optional<Patient> result = useCase.execute(patientId);

        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    @DisplayName("retorna Optional vazio quando paciente não é encontrado")
    void execute_missingPatient_returnsEmptyOptional() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        Optional<Patient> result = useCase.execute(patientId);

        assertTrue(result.isEmpty());
    }
}

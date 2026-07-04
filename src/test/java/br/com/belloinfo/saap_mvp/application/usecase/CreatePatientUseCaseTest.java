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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePatientUseCaseTest {

    @Mock
    private PatientRepository patientRepository;

    private CreatePatientUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePatientUseCase(patientRepository);
    }

    private Patient patientWithCpf(String cpf) {
        return Patient.builder()
                .name("Maria Silva")
                .cpf(cpf)
                .build();
    }

    @Test
    @DisplayName("cria paciente ativando-o e persistindo com CPF normalizado")
    void execute_validPatient_activatesAndSavesWithNormalizedCpf() {
        Patient patient = patientWithCpf("697.028.342-95");
        when(patientRepository.findByCpf("69702834295")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = useCase.execute(patient);

        assertEquals("69702834295", result.getCpf());
        assertTrue(result.isActive());
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("aceita CPF já sem formatação")
    void execute_unformattedCpf_savesNormally() {
        Patient patient = patientWithCpf("69702834295");
        when(patientRepository.findByCpf("69702834295")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = useCase.execute(patient);

        assertEquals("69702834295", result.getCpf());
        verify(patientRepository).findByCpf("69702834295");
    }

    @Test
    @DisplayName("permite cadastro sem CPF, ignorando verificação de duplicidade")
    void execute_nullCpf_skipsDuplicateCheckAndSaves() {
        Patient patient = patientWithCpf(null);
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = useCase.execute(patient);

        assertNull(result.getCpf());
        assertTrue(result.isActive());
        verify(patientRepository, never()).findByCpf(any());
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("rejeita cadastro com CPF já existente")
    void execute_duplicateCpf_throwsIllegalArgumentException() {
        Patient patient = patientWithCpf("69702834295");
        when(patientRepository.findByCpf("69702834295"))
                .thenReturn(Optional.of(patientWithCpf("69702834295")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(patient));

        verify(patientRepository, never()).save(any());
    }
}

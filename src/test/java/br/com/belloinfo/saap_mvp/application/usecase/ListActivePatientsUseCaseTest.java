package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListActivePatientsUseCaseTest {

    @Mock
    private PatientRepository patientRepository;

    private ListActivePatientsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActivePatientsUseCase(patientRepository);
    }

    private Patient patient(String name, boolean active) {
        return Patient.builder().id(UUID.randomUUID()).name(name).active(active).build();
    }

    @Test
    @DisplayName("retorna apenas pacientes ativos, filtrando os inativos")
    void execute_mixedPatients_returnsOnlyActiveOnes() {
        Patient active1 = patient("Maria Silva", true);
        Patient inactive = patient("João Souza", false);
        Patient active2 = patient("Ana Costa", true);
        when(patientRepository.findAll()).thenReturn(List.of(active1, inactive, active2));

        List<Patient> result = useCase.execute();

        assertEquals(2, result.size());
        assertTrue(result.contains(active1));
        assertTrue(result.contains(active2));
        assertFalse(result.contains(inactive));
    }

    @Test
    @DisplayName("retorna lista vazia quando não há pacientes cadastrados")
    void execute_noPatients_returnsEmptyList() {
        when(patientRepository.findAll()).thenReturn(List.of());

        List<Patient> result = useCase.execute();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("retorna lista vazia quando todos os pacientes estão inativos")
    void execute_allInactivePatients_returnsEmptyList() {
        when(patientRepository.findAll()).thenReturn(List.of(patient("Maria Silva", false), patient("João Souza", false)));

        List<Patient> result = useCase.execute();

        assertTrue(result.isEmpty());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
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
    @DisplayName("delega paginação ao repositório e retorna o PageResult obtido")
    void execute_delegatesToRepositoryWithPageAndSize() {
        Patient active1 = patient("Maria Silva", true);
        Patient active2 = patient("Ana Costa", true);
        PageResult<Patient> expected = new PageResult<>(List.of(active1, active2), 0, 20, 2, 1);
        when(patientRepository.findActive(0, 20)).thenReturn(expected);

        PageResult<Patient> result = useCase.execute(0, 20);

        assertEquals(expected, result);
        verify(patientRepository).findActive(0, 20);
    }

    @Test
    @DisplayName("retorna PageResult vazio quando não há pacientes cadastrados")
    void execute_noPatients_returnsEmptyPageResult() {
        PageResult<Patient> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(patientRepository.findActive(0, 20)).thenReturn(expected);

        PageResult<Patient> result = useCase.execute(0, 20);

        assertTrue(result.content().isEmpty());
    }
}

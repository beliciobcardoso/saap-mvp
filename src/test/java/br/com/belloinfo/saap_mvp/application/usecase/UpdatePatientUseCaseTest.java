package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
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
class UpdatePatientUseCaseTest {

    @Mock
    private PatientRepository patientRepository;

    private UpdatePatientUseCase useCase;

    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdatePatientUseCase(patientRepository);
    }

    private Patient existingPatient(String cpf) {
        return Patient.builder()
                .id(patientId)
                .name("Maria Silva")
                .cpf(cpf)
                .email("maria@example.com")
                .build();
    }

    private Patient updatedData(String cpf) {
        return Patient.builder()
                .name("Maria Silva Santos")
                .cpf(cpf)
                .susNumber("123456789012345")
                .email("maria.santos@example.com")
                .phone("11999998888")
                .build();
    }

    @Test
    @DisplayName("atualiza paciente existente com CPF novo e único")
    void execute_existingPatientWithNewUniqueCpf_updatesAndSaves() {
        Patient existing = existingPatient("11111111111");
        Patient updated = updatedData("697.028.342-95");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.findByCpf("69702834295")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = useCase.execute(patientId, updated);

        assertEquals("Maria Silva Santos", result.getName());
        assertEquals("69702834295", result.getCpf());
        assertEquals("maria.santos@example.com", result.getEmail());
        verify(patientRepository).save(existing);
    }

    @Test
    @DisplayName("lança exceção quando paciente não é encontrado")
    void execute_patientNotFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(patientId, updatedData("69702834295")));

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("permite manter o mesmo CPF sem disparar verificação de duplicidade")
    void execute_sameCpfAsExisting_doesNotCheckDuplicateAndSaves() {
        Patient existing = existingPatient("69702834295");
        Patient updated = updatedData("697.028.342-95");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = useCase.execute(patientId, updated);

        assertEquals("69702834295", result.getCpf());
        verify(patientRepository, never()).findByCpf(any());
        verify(patientRepository).save(existing);
    }

    @Test
    @DisplayName("rejeita atualização com CPF já usado por outro paciente")
    void execute_cpfBelongsToAnotherPatient_throwsIllegalArgumentException() {
        Patient existing = existingPatient("11111111111");
        Patient another = existingPatient("69702834295");
        Patient updated = updatedData("69702834295");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.findByCpf("69702834295")).thenReturn(Optional.of(another));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(patientId, updated));

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("permite atualização sem CPF, ignorando verificação de duplicidade")
    void execute_nullCpf_skipsDuplicateCheckAndSaves() {
        Patient existing = existingPatient("11111111111");
        Patient updated = updatedData(null);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = useCase.execute(patientId, updated);

        assertNull(result.getCpf());
        verify(patientRepository, never()).findByCpf(any());
        verify(patientRepository).save(existing);
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
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
class CreateProfessionalUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    private CreateProfessionalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateProfessionalUseCase(professionalRepository);
    }

    private Professional newProfessional(String registrationNumber) {
        return Professional.builder()
                .name("Dra. Ana Souza")
                .email("ana.souza@example.com")
                .phone("11999999999")
                .registrationNumber(registrationNumber)
                .role(ProfessionalRole.PROFESSIONAL)
                .build();
    }

    @Test
    @DisplayName("cria profissional ativando-o e persistindo quando registro é inédito")
    void execute_withUniqueRegistrationNumber_createsActiveProfessional() {
        Professional professional = newProfessional("CRM-1234");
        when(professionalRepository.findByRegistrationNumber("CRM-1234")).thenReturn(Optional.empty());
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        Professional created = useCase.execute(professional);

        assertTrue(created.isActive());
        verify(professionalRepository).save(professional);
    }

    @Test
    @DisplayName("cria profissional sem registro profissional informado, ignorando verificação de duplicidade")
    void execute_withoutRegistrationNumber_skipsDuplicateCheckAndCreates() {
        Professional professional = newProfessional(null);
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));

        Professional created = useCase.execute(professional);

        assertTrue(created.isActive());
        verify(professionalRepository, never()).findByRegistrationNumber(any());
        verify(professionalRepository).save(professional);
    }

    @Test
    @DisplayName("rejeita criação quando registro profissional já está cadastrado")
    void execute_withDuplicateRegistrationNumber_throwsIllegalArgumentException() {
        Professional professional = newProfessional("CRM-1234");
        when(professionalRepository.findByRegistrationNumber("CRM-1234"))
                .thenReturn(Optional.of(newProfessional("CRM-1234")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(professional));

        verify(professionalRepository, never()).save(any());
    }
}

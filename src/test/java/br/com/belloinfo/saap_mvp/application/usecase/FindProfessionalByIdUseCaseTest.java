package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindProfessionalByIdUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    private FindProfessionalByIdUseCase useCase;

    private final UUID professionalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new FindProfessionalByIdUseCase(professionalRepository);
    }

    @Test
    @DisplayName("retorna profissional encapsulado em Optional quando encontrado")
    void execute_existingProfessional_returnsPresentOptional() {
        Professional professional = Professional.builder().id(professionalId).name("Dr. Pedro Alves").build();
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        Optional<Professional> result = useCase.execute(professionalId);

        assertTrue(result.isPresent());
        assertEquals(professionalId, result.get().getId());
    }

    @Test
    @DisplayName("retorna Optional vazio quando profissional não existe")
    void execute_nonExistingProfessional_returnsEmptyOptional() {
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.empty());

        Optional<Professional> result = useCase.execute(professionalId);

        assertTrue(result.isEmpty());
    }
}

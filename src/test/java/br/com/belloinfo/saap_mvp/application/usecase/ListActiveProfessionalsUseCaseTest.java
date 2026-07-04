package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActiveProfessionalsUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    private ListActiveProfessionalsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActiveProfessionalsUseCase(professionalRepository);
    }

    @Test
    @DisplayName("retorna lista de profissionais ativos quando existem registros")
    void execute_withActiveProfessionals_returnsList() {
        Professional first = Professional.builder().name("Dra. Marina Costa").active(true).build();
        Professional second = Professional.builder().name("Dr. Rafael Nunes").active(true).build();
        when(professionalRepository.findAllActive()).thenReturn(List.of(first, second));

        List<Professional> result = useCase.execute();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(first, second)));
    }

    @Test
    @DisplayName("retorna lista vazia quando não há profissionais ativos")
    void execute_withoutActiveProfessionals_returnsEmptyList() {
        when(professionalRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Professional> result = useCase.execute();

        assertTrue(result.isEmpty());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
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
import static org.mockito.Mockito.verify;
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
    @DisplayName("retorna PageResult de profissionais ativos quando existem registros")
    void execute_withActiveProfessionals_returnsPageResult() {
        Professional first = Professional.builder().name("Dra. Marina Costa").active(true).build();
        Professional second = Professional.builder().name("Dr. Rafael Nunes").active(true).build();
        PageResult<Professional> expected = new PageResult<>(List.of(first, second), 0, 20, 2, 1);
        when(professionalRepository.findActive(0, 20)).thenReturn(expected);

        PageResult<Professional> result = useCase.execute(0, 20);

        assertEquals(2, result.content().size());
        assertTrue(result.content().containsAll(List.of(first, second)));
        verify(professionalRepository).findActive(0, 20);
    }

    @Test
    @DisplayName("retorna PageResult vazio quando não há profissionais ativos")
    void execute_withoutActiveProfessionals_returnsEmptyPageResult() {
        when(professionalRepository.findActive(0, 20)).thenReturn(new PageResult<>(Collections.emptyList(), 0, 20, 0, 0));

        PageResult<Professional> result = useCase.execute(0, 20);

        assertTrue(result.content().isEmpty());
    }
}

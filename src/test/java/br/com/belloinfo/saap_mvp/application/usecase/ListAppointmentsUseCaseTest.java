package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListAppointmentsUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private ListAppointmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAppointmentsUseCase(appointmentRepository);
    }

    @Test
    @DisplayName("retorna agendamentos filtrados por profissional, paciente e período")
    void execute_withFilters_returnsMatchingAppointments() {
        UUID professionalId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 31, 23, 59);
        PageResult<Appointment> expected = new PageResult<>(
                List.of(Appointment.builder().id(UUID.randomUUID()).status(AppointmentStatus.CONFIRMED).build()),
                0, 20, 1, 1);
        when(appointmentRepository.findByFilters(professionalId, patientId, start, end, 0, 20)).thenReturn(expected);

        PageResult<Appointment> result = useCase.execute(professionalId, patientId, start, end, 0, 20);

        assertEquals(expected, result);
        verify(appointmentRepository).findByFilters(professionalId, patientId, start, end, 0, 20);
    }

    @Test
    @DisplayName("retorna PageResult vazio quando nenhum agendamento corresponde aos filtros")
    void execute_noMatches_returnsEmptyPageResult() {
        when(appointmentRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0, 20, 0, 0));

        PageResult<Appointment> result = useCase.execute(null, null, null, null, 0, 20);

        assertTrue(result.content().isEmpty());
    }
}

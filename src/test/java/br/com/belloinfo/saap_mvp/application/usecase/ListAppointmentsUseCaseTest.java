package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
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
        List<Appointment> expected = List.of(
                Appointment.builder().id(UUID.randomUUID()).status(AppointmentStatus.CONFIRMED).build());
        when(appointmentRepository.findByFilters(professionalId, patientId, start, end)).thenReturn(expected);

        List<Appointment> result = useCase.execute(professionalId, patientId, start, end);

        assertEquals(expected, result);
        verify(appointmentRepository).findByFilters(professionalId, patientId, start, end);
    }

    @Test
    @DisplayName("retorna lista vazia quando nenhum agendamento corresponde aos filtros")
    void execute_noMatches_returnsEmptyList() {
        when(appointmentRepository.findByFilters(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        List<Appointment> result = useCase.execute(null, null, null, null);

        assertTrue(result.isEmpty());
    }
}

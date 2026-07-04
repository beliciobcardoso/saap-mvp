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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private ConfirmAppointmentUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ConfirmAppointmentUseCase(appointmentRepository);
    }

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        return Appointment.builder()
                .id(appointmentId)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("confirma agendamento pendente com sucesso")
    void execute_pendingAppointment_confirmsSuccessfully() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.PENDING)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        verify(appointmentRepository).save(result);
    }

    @Test
    @DisplayName("confirma agendamento em pending_response com sucesso")
    void execute_pendingResponseAppointment_confirmsSuccessfully() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.PENDING_RESPONSE)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
    }

    @Test
    @DisplayName("lança exceção quando agendamento não é encontrado")
    void execute_appointmentNotFound_throwsException() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("lança exceção ao tentar confirmar agendamento em status terminal")
    void execute_terminalStatus_throwsIllegalStateException() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.CANCELLED)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
    }
}

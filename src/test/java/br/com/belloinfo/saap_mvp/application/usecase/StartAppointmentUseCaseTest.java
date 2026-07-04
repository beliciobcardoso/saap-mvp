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
class StartAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private StartAppointmentUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new StartAppointmentUseCase(appointmentRepository);
    }

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        return Appointment.builder().id(appointmentId).status(status).build();
    }

    @Test
    @DisplayName("inicia atendimento quando agendamento está sendo chamado")
    void execute_callingAppointment_startsAppointment() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.CALLING)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.IN_PROGRESS, result.getStatus());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("rejeita início quando agendamento não é encontrado")
    void execute_appointmentNotFound_throwsIllegalArgumentException() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(appointmentId));

        assertEquals("Agendamento não encontrado", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita início quando agendamento não está em estado de chamada")
    void execute_appointmentNotCalling_throwsIllegalStateException() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.PENDING)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProcessWaitlistSlotOfferUseCase processWaitlistSlotOfferUseCase;

    private CancelAppointmentUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();
    private final UUID professionalId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CancelAppointmentUseCase(appointmentRepository, processWaitlistSlotOfferUseCase);
        ReflectionTestUtils.setField(useCase, "waitlistAutoFill", true);
    }

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        return Appointment.builder()
                .id(appointmentId)
                .status(status)
                .professional(Professional.builder().id(professionalId).build())
                .service(Service.builder().id(serviceId).build())
                .dateTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    @DisplayName("cancela agendamento pendente com sucesso e preenche fila de espera")
    void execute_pendingAppointment_cancelsAndFillsWaitlist() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.PENDING)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        verify(appointmentRepository).save(result);
        verify(processWaitlistSlotOfferUseCase).execute(professionalId, serviceId, result.getDateTime());
    }

    @Test
    @DisplayName("cancela agendamento confirmado com sucesso")
    void execute_confirmedAppointment_cancelsSuccessfully() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.CONFIRMED)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        verify(processWaitlistSlotOfferUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("não preenche fila de espera quando waitlistAutoFill está desabilitado")
    void execute_waitlistAutoFillDisabled_doesNotFillWaitlist() {
        ReflectionTestUtils.setField(useCase, "waitlistAutoFill", false);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.PENDING)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        verify(processWaitlistSlotOfferUseCase, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("lança exceção quando agendamento não é encontrado")
    void execute_appointmentNotFound_throwsException() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
        verify(processWaitlistSlotOfferUseCase, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("lança exceção ao tentar cancelar agendamento já completado")
    void execute_completedAppointment_throwsIllegalStateException() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointmentWithStatus(AppointmentStatus.COMPLETED)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar agendamento já cancelado é no-op (mesmo estado)")
    void execute_alreadyCancelledAppointment_isNoOp() {
        Appointment existing = appointmentWithStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        verify(appointmentRepository).save(result);
        verify(processWaitlistSlotOfferUseCase).execute(professionalId, serviceId, result.getDateTime());
    }
}

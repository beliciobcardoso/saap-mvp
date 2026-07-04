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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAppointmentByIdUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private FindAppointmentByIdUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new FindAppointmentByIdUseCase(appointmentRepository);
    }

    @Test
    @DisplayName("retorna agendamento quando encontrado pelo id")
    void execute_existingAppointment_returnsAppointment() {
        Appointment appointment = Appointment.builder().id(appointmentId).status(AppointmentStatus.PENDING).build();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Optional<Appointment> result = useCase.execute(appointmentId);

        assertTrue(result.isPresent());
        assertEquals(appointmentId, result.get().getId());
    }

    @Test
    @DisplayName("retorna Optional vazio quando agendamento não é encontrado")
    void execute_missingAppointment_returnsEmptyOptional() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        Optional<Appointment> result = useCase.execute(appointmentId);

        assertTrue(result.isEmpty());
    }
}

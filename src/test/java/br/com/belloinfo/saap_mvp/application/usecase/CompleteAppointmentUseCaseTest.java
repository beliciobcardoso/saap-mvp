package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.MedicalRecordConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
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
class CompleteAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicalRecordEntryRepository medicalRecordEntryRepository;

    private CompleteAppointmentUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CompleteAppointmentUseCase(appointmentRepository, medicalRecordEntryRepository);
    }

    private Appointment inProgressAppointment() {
        return Appointment.builder()
                .id(appointmentId)
                .status(AppointmentStatus.IN_PROGRESS)
                .build();
    }

    @Test
    @DisplayName("finaliza atendimento com evolução preenchida")
    void execute_withEvolution_completesAppointment() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(inProgressAppointment()));
        when(medicalRecordEntryRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(MedicalRecordEntry.builder().evolution("evolução preenchida").build()));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment completed = useCase.execute(appointmentId);

        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
    }

    @Test
    @DisplayName("rejeita finalização sem entrada de evolução")
    void execute_withoutEntry_throwsConflict() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(inProgressAppointment()));
        when(medicalRecordEntryRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordConflictException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita finalização com evolução em branco")
    void execute_withBlankEvolution_throwsConflict() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(inProgressAppointment()));
        when(medicalRecordEntryRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(MedicalRecordEntry.builder().evolution("   ").build()));

        assertThrows(MedicalRecordConflictException.class, () -> useCase.execute(appointmentId));

        verify(appointmentRepository, never()).save(any());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.MedicalRecordConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateMedicalRecordEntryUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private MedicalRecordEntryRepository medicalRecordEntryRepository;

    private CreateMedicalRecordEntryUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID professionalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CreateMedicalRecordEntryUseCase(
                appointmentRepository, medicalRecordRepository, medicalRecordEntryRepository);
    }

    private Appointment appointment(AppointmentStatus status) {
        return Appointment.builder()
                .id(appointmentId)
                .status(status)
                .patient(Patient.builder().id(patientId).build())
                .professional(Professional.builder().id(professionalId).build())
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
    @DisplayName("rejeita criação de evolução quando agendamento não está IN_PROGRESS")
    void execute_appointmentNotInProgress_throwsConflict(AppointmentStatus status) {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment(status)));

        assertThrows(MedicalRecordConflictException.class,
                () -> useCase.execute(appointmentId, "evolução", professionalId));

        verify(medicalRecordEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita criação por profissional diferente do agendamento")
    void execute_divergentProfessional_throwsAccessDenied() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(appointmentId, "evolução", UUID.randomUUID()));

        verify(medicalRecordEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita segunda entrada para o mesmo agendamento")
    void execute_duplicateEntry_throwsConflict() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));
        when(medicalRecordEntryRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(MedicalRecordEntry.builder().build()));

        assertThrows(MedicalRecordConflictException.class,
                () -> useCase.execute(appointmentId, "evolução", professionalId));

        verify(medicalRecordEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("cria prontuário sob demanda quando paciente ainda não possui")
    void execute_patientWithoutRecord_createsRecordLazily() {
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));
        when(medicalRecordEntryRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByPatientId(patientId)).thenReturn(Optional.empty());

        UUID recordId = UUID.randomUUID();
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(inv -> {
                    MedicalRecord record = inv.getArgument(0);
                    record.setId(recordId);
                    return record;
                });
        when(medicalRecordEntryRepository.save(any(MedicalRecordEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalRecordEntry entry = useCase.execute(appointmentId, "evolução do paciente", professionalId);

        verify(medicalRecordRepository).save(any(MedicalRecord.class));
        assertEquals(recordId, entry.getMedicalRecordId());
        assertEquals(appointmentId, entry.getAppointmentId());
        assertEquals(professionalId, entry.getProfessionalId());
        assertEquals("evolução do paciente", entry.getEvolution());
    }

    @Test
    @DisplayName("reutiliza prontuário existente sem criar um novo")
    void execute_patientWithRecord_reusesExistingRecord() {
        UUID recordId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));
        when(medicalRecordEntryRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByPatientId(patientId))
                .thenReturn(Optional.of(MedicalRecord.builder().id(recordId).patientId(patientId).build()));
        when(medicalRecordEntryRepository.save(any(MedicalRecordEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalRecordEntry entry = useCase.execute(appointmentId, "evolução", professionalId);

        verify(medicalRecordRepository, never()).save(any());
        assertEquals(recordId, entry.getMedicalRecordId());
    }
}

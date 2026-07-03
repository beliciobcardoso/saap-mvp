package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.MedicalRecordConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
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
class UpdateMedicalRecordEntryUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicalRecordEntryRepository medicalRecordEntryRepository;

    private UpdateMedicalRecordEntryUseCase useCase;

    private final UUID entryId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();
    private final UUID professionalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdateMedicalRecordEntryUseCase(appointmentRepository, medicalRecordEntryRepository);
    }

    private MedicalRecordEntry entry() {
        return MedicalRecordEntry.builder()
                .id(entryId)
                .appointmentId(appointmentId)
                .professionalId(professionalId)
                .evolution("evolução original")
                .build();
    }

    private Appointment appointment(AppointmentStatus status) {
        return Appointment.builder()
                .id(appointmentId)
                .status(status)
                .professional(Professional.builder().id(professionalId).build())
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
    @DisplayName("entrada é imutável quando agendamento não está IN_PROGRESS")
    void execute_appointmentNotInProgress_throwsConflict(AppointmentStatus status) {
        when(medicalRecordEntryRepository.findById(entryId)).thenReturn(Optional.of(entry()));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment(status)));

        assertThrows(MedicalRecordConflictException.class,
                () -> useCase.execute(entryId, "nova evolução", professionalId));

        verify(medicalRecordEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita edição por profissional diferente do agendamento")
    void execute_divergentProfessional_throwsAccessDenied() {
        when(medicalRecordEntryRepository.findById(entryId)).thenReturn(Optional.of(entry()));
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(entryId, "nova evolução", UUID.randomUUID()));

        verify(medicalRecordEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualiza evolução dentro da janela IN_PROGRESS")
    void execute_inProgress_updatesEvolution() {
        when(medicalRecordEntryRepository.findById(entryId)).thenReturn(Optional.of(entry()));
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(AppointmentStatus.IN_PROGRESS)));
        when(medicalRecordEntryRepository.save(any(MedicalRecordEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalRecordEntry updated = useCase.execute(entryId, "nova evolução", professionalId);

        assertEquals("nova evolução", updated.getEvolution());
        verify(medicalRecordEntryRepository).save(any(MedicalRecordEntry.class));
    }
}

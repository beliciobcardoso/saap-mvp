package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.MedicalRecordConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateMedicalRecordEntryUseCase {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordEntryRepository medicalRecordEntryRepository;

    @Transactional
    public MedicalRecordEntry execute(UUID appointmentId, String evolution, UUID currentProfessionalId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new MedicalRecordConflictException(
                    "Evolução clínica só pode ser registrada com o atendimento em andamento (IN_PROGRESS)");
        }

        if (!appointment.getProfessional().getId().equals(currentProfessionalId)) {
            throw new AccessDeniedException("Apenas o profissional do atendimento pode registrar a evolução");
        }

        if (medicalRecordEntryRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new MedicalRecordConflictException("Este agendamento já possui uma entrada de evolução");
        }

        MedicalRecord medicalRecord = medicalRecordRepository
                .findByPatientId(appointment.getPatient().getId())
                .orElseGet(() -> medicalRecordRepository.save(MedicalRecord.builder()
                        .patientId(appointment.getPatient().getId())
                        .build()));

        MedicalRecordEntry entry = MedicalRecordEntry.builder()
                .medicalRecordId(medicalRecord.getId())
                .appointmentId(appointmentId)
                .professionalId(currentProfessionalId)
                .evolution(evolution)
                .build();

        return medicalRecordEntryRepository.save(entry);
    }
}

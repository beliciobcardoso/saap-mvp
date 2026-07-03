package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.MedicalRecordConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateMedicalRecordEntryUseCase {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordEntryRepository medicalRecordEntryRepository;

    @Transactional
    public MedicalRecordEntry execute(UUID entryId, String evolution, UUID currentProfessionalId) {
        MedicalRecordEntry entry = medicalRecordEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entrada de prontuário não encontrada"));

        Appointment appointment = appointmentRepository.findById(entry.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento da entrada não encontrado"));

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new MedicalRecordConflictException(
                    "Evolução clínica imutável: o atendimento não está mais em andamento (IN_PROGRESS)");
        }

        if (!appointment.getProfessional().getId().equals(currentProfessionalId)) {
            throw new AccessDeniedException("Apenas o profissional do atendimento pode editar a evolução");
        }

        entry.setEvolution(evolution);
        return medicalRecordEntryRepository.save(entry);
    }
}

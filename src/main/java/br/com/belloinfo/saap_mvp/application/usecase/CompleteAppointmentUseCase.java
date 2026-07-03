package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.MedicalRecordConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordEntryRepository medicalRecordEntryRepository;

    @Transactional
    public Appointment execute(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        boolean hasEvolution = medicalRecordEntryRepository.findByAppointmentId(appointmentId)
                .map(entry -> entry.getEvolution() != null && !entry.getEvolution().isBlank())
                .orElse(false);
        if (!hasEvolution) {
            throw new MedicalRecordConflictException(
                    "Não é possível finalizar o atendimento sem a evolução clínica preenchida");
        }

        appointment.transitionTo(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }
}

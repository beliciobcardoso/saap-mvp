package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckInAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public Appointment execute(UUID appointmentId, PriorityLevel verifiedLevel, UUID verifiedBy, String notes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        appointment.checkIn(verifiedLevel, verifiedBy, notes, System.currentTimeMillis());
        return appointmentRepository.save(appointment);
    }
}

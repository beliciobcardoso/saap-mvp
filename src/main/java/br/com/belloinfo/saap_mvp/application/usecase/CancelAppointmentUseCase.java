package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ProcessWaitlistSlotOfferUseCase processWaitlistSlotOfferUseCase;

    @org.springframework.beans.factory.annotation.Value("${saap.waitlist.auto-fill:true}")
    private boolean waitlistAutoFill = true;

    @Transactional
    public Appointment execute(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        appointment.transitionTo(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        if (waitlistAutoFill) {
            processWaitlistSlotOfferUseCase.execute(
                    savedAppointment.getProfessional().getId(),
                    savedAppointment.getService().getId(),
                    savedAppointment.getDateTime()
            );
        }

        return savedAppointment;
    }
}

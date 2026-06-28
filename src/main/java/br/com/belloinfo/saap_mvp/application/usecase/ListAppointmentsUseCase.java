package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListAppointmentsUseCase {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<Appointment> execute(UUID professionalId, UUID patientId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return appointmentRepository.findByFilters(professionalId, patientId, startDateTime, endDateTime);
    }
}

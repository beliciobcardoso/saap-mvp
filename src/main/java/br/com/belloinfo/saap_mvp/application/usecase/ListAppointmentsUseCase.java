package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListAppointmentsUseCase {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public PageResult<Appointment> execute(UUID professionalId, UUID patientId, LocalDateTime startDateTime, LocalDateTime endDateTime, int page, int size) {
        return appointmentRepository.findByFilters(professionalId, patientId, startDateTime, endDateTime, page, size);
    }
}

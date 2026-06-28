package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindAppointmentByIdUseCase {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public Optional<Appointment> execute(UUID id) {
        return appointmentRepository.findById(id);
    }
}

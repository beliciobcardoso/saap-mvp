package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    List<Appointment> findAll();
    boolean existsByProfessionalIdAndDateTimeAndStatusNotIn(UUID professionalId, LocalDateTime dateTime, List<AppointmentStatus> statuses);
    List<Appointment> findByFilters(UUID professionalId, UUID patientId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    List<Appointment> findByStatusAndDateTimeBetweenAndFollowUpSentFalse(AppointmentStatus status, LocalDateTime startDateTime, LocalDateTime endDateTime);
}

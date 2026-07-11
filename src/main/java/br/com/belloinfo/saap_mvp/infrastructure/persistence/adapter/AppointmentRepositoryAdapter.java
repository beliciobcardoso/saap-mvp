package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.PaginationSupport;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AppointmentEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaAppointmentRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaPatientRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaProfessionalRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AppointmentRepositoryAdapter implements AppointmentRepository {

    private final JpaAppointmentRepository jpaAppointmentRepository;
    private final JpaPatientRepository jpaPatientRepository;
    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final JpaServiceRepository jpaServiceRepository;
    private final CoreMapper mapper;

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = mapper.toEntity(appointment);
        
        // Link entities via references to avoid TransientPropertyValueException
        if (appointment.getPatient() != null && appointment.getPatient().getId() != null) {
            entity.setPatient(jpaPatientRepository.getReferenceById(appointment.getPatient().getId()));
        }
        if (appointment.getProfessional() != null && appointment.getProfessional().getId() != null) {
            entity.setProfessional(jpaProfessionalRepository.getReferenceById(appointment.getProfessional().getId()));
        }
        if (appointment.getService() != null && appointment.getService().getId() != null) {
            entity.setService(jpaServiceRepository.getReferenceById(appointment.getService().getId()));
        }

        AppointmentEntity savedEntity = jpaAppointmentRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaAppointmentRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByProfessionalIdAndDateTimeAndStatusNotIn(UUID professionalId, LocalDateTime dateTime, List<AppointmentStatus> statuses) {
        return jpaAppointmentRepository.existsByProfessionalIdAndDateTimeAndStatusNotIn(professionalId, dateTime, statuses);
    }

    @Override
    public PageResult<Appointment> findByFilters(UUID professionalId, UUID patientId, LocalDateTime startDateTime, LocalDateTime endDateTime, int page, int size) {
        return PaginationSupport.toPageResult(
                jpaAppointmentRepository.findByFilters(professionalId, patientId, startDateTime, endDateTime, PaginationSupport.of(page, size)),
                mapper::toDomain
        );
    }

    @Override
    public Optional<Appointment> findNextInQueueWithLock(UUID professionalId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.findNextInQueueWithLock(
                professionalId,
                start,
                end
        ).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findEligibleForFollowUp(LocalDateTime windowStart, LocalDateTime windowEnd) {
        return jpaAppointmentRepository.findEligibleForFollowUp(windowStart, windowEnd).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findPendingResponsePastDeadline(LocalDateTime deadline) {
        return jpaAppointmentRepository.findPendingResponsePastDeadline(deadline).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPatientIdAndProfessionalId(UUID patientId, UUID professionalId) {
        return jpaAppointmentRepository.existsByPatientIdAndProfessionalId(patientId, professionalId);
    }
}

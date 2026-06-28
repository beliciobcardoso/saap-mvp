package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
    
    boolean existsByProfessionalIdAndDateTimeAndStatusNotIn(UUID professionalId, LocalDateTime dateTime, List<AppointmentStatus> statuses);

    @Query("SELECT a FROM AppointmentEntity a WHERE " +
           "(:professionalId IS NULL OR a.professional.id = :professionalId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(CAST(:start AS timestamp) IS NULL OR a.dateTime >= :start) AND " +
           "(CAST(:end AS timestamp) IS NULL OR a.dateTime <= :end)")
    List<AppointmentEntity> findByFilters(
            @Param("professionalId") UUID professionalId,
            @Param("patientId") UUID patientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<AppointmentEntity> findByStatusAndDateTimeBetweenAndFollowUpSentFalse(
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}

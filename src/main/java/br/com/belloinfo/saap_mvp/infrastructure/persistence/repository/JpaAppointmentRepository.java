package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
    
    boolean existsByProfessionalIdAndDateTimeAndStatusNotIn(UUID professionalId, LocalDateTime dateTime, List<AppointmentStatus> statuses);

    @Query("SELECT a FROM AppointmentEntity a WHERE " +
           "(:professionalId IS NULL OR a.professional.id = :professionalId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(CAST(:start AS timestamp) IS NULL OR a.dateTime >= :start) AND " +
           "(CAST(:end AS timestamp) IS NULL OR a.dateTime <= :end)")
    Page<AppointmentEntity> findByFilters(
            @Param("professionalId") UUID professionalId,
            @Param("patientId") UUID patientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    List<AppointmentEntity> findByStatusAndDateTimeBetweenAndFollowUpSentFalse(
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<AppointmentEntity> findFirstByProfessionalIdAndStatusAndDateTimeBetweenOrderByPriorityScoreAsc(
            UUID professionalId,
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT a FROM AppointmentEntity a WHERE " +
           "a.status = 'PENDING' AND " +
           "a.followUpSentAt IS NULL AND " +
           "a.dateTime >= :windowStart AND " +
           "a.dateTime <= :windowEnd")
    List<AppointmentEntity> findEligibleForFollowUp(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    @Query("SELECT a FROM AppointmentEntity a WHERE " +
           "a.status = 'PENDING_RESPONSE' AND " +
           "a.dateTime <= :deadline")
    List<AppointmentEntity> findPendingResponsePastDeadline(
            @Param("deadline") LocalDateTime deadline
    );
}

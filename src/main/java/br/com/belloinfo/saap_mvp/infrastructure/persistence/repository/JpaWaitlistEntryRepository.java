package br.com.belloinfo.saap_mvp.infrastructure.persistence.repository;

import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.WaitlistEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaWaitlistEntryRepository extends JpaRepository<WaitlistEntryEntity, UUID> {
    List<WaitlistEntryEntity> findByProfessionalIdAndServiceIdAndActiveTrueAndStatusOrderByCreatedAtAsc(
            UUID professionalId, UUID serviceId, WaitlistStatus status);

    List<WaitlistEntryEntity> findByActiveTrueAndStatusAndOfferExpiresAtBefore(WaitlistStatus status, LocalDateTime now);

    List<WaitlistEntryEntity> findByStatusAndOfferedAppointmentTimeAndActiveTrue(WaitlistStatus status, LocalDateTime offeredAppointmentTime);

    Optional<WaitlistEntryEntity> findFirstByPatientPhoneAndStatusAndActiveTrueOrderByOfferedAppointmentTimeDesc(
            String phone, WaitlistStatus status);
}

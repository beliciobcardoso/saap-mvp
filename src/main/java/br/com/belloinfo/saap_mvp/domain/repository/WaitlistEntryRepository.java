package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistEntryRepository {
    WaitlistEntry save(WaitlistEntry entry);
    Optional<WaitlistEntry> findById(UUID id);
    List<WaitlistEntry> findActiveByProfessionalAndServiceOrderByCreatedAtAsc(UUID professionalId, UUID serviceId);
    List<WaitlistEntry> findActiveOffersExpired(LocalDateTime now);
    List<WaitlistEntry> findByStatusAndOfferedAppointmentTimeAndActiveTrue(WaitlistStatus status, LocalDateTime offeredAppointmentTime);
    Optional<WaitlistEntry> findMostRecentOfferedByPatientPhone(String phone);
}

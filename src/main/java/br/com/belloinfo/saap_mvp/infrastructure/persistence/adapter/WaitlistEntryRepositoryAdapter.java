package br.com.belloinfo.saap_mvp.infrastructure.persistence.adapter;

import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.WaitlistEntryEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper.CoreMapper;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaPatientRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaProfessionalRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaServiceRepository;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaWaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WaitlistEntryRepositoryAdapter implements WaitlistEntryRepository {

    private final JpaWaitlistEntryRepository jpaWaitlistEntryRepository;
    private final JpaPatientRepository jpaPatientRepository;
    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final JpaServiceRepository jpaServiceRepository;
    private final CoreMapper mapper;

    @Override
    public WaitlistEntry save(WaitlistEntry entry) {
        WaitlistEntryEntity entity = mapper.toEntity(entry);

        if (entry.getPatient() != null && entry.getPatient().getId() != null) {
            entity.setPatient(jpaPatientRepository.getReferenceById(entry.getPatient().getId()));
        }
        if (entry.getProfessional() != null && entry.getProfessional().getId() != null) {
            entity.setProfessional(jpaProfessionalRepository.getReferenceById(entry.getProfessional().getId()));
        }
        if (entry.getService() != null && entry.getService().getId() != null) {
            entity.setService(jpaServiceRepository.getReferenceById(entry.getService().getId()));
        }

        WaitlistEntryEntity saved = jpaWaitlistEntryRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WaitlistEntry> findById(UUID id) {
        return jpaWaitlistEntryRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WaitlistEntry> findActiveByProfessionalAndServiceOrderByCreatedAtAsc(UUID professionalId, UUID serviceId) {
        return jpaWaitlistEntryRepository.findByProfessionalIdAndServiceIdAndActiveTrueAndStatusOrderByCreatedAtAsc(
                professionalId, serviceId, WaitlistStatus.WAITING
        ).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<WaitlistEntry> findActiveOffersExpired(LocalDateTime now) {
        return jpaWaitlistEntryRepository.findByActiveTrueAndStatusAndOfferExpiresAtBefore(
                WaitlistStatus.OFFERED, now
        ).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<WaitlistEntry> findByStatusAndOfferedAppointmentTimeAndActiveTrue(WaitlistStatus status, LocalDateTime offeredAppointmentTime) {
        return jpaWaitlistEntryRepository.findByStatusAndOfferedAppointmentTimeAndActiveTrue(
                status, offeredAppointmentTime
        ).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<WaitlistEntry> findMostRecentOfferedByPatientPhone(String phone) {
        return jpaWaitlistEntryRepository.findFirstByPatientPhoneAndStatusAndActiveTrueOrderByOfferedAppointmentTimeDesc(
                phone, WaitlistStatus.OFFERED
        ).map(mapper::toDomain);
    }
}

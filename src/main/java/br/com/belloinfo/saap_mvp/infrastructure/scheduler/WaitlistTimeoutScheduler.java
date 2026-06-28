package br.com.belloinfo.saap_mvp.infrastructure.scheduler;

import br.com.belloinfo.saap_mvp.application.usecase.ProcessWaitlistSlotOfferUseCase;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WaitlistTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaitlistTimeoutScheduler.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ProcessWaitlistSlotOfferUseCase processWaitlistSlotOfferUseCase;

    @Scheduled(cron = "${saap.scheduler.waitlist.cron:0 * * * * *}") // a cada 1 minuto
    @Transactional
    public void runWaitlistTimeoutJob() {
        log.trace("Verificando ofertas expiradas na fila de espera...");
        List<WaitlistEntry> expiredEntries = waitlistEntryRepository.findActiveOffersExpired(LocalDateTime.now());

        for (WaitlistEntry entry : expiredEntries) {
            log.info("Oferta da fila de espera {} para o paciente {} expirou por timeout.", 
                    entry.getId(), entry.getPatient().getName());
            
            LocalDateTime offeredTime = entry.getOfferedAppointmentTime();
            var professionalId = entry.getProfessional().getId();
            var serviceId = entry.getService().getId();

            entry.expire();
            waitlistEntryRepository.save(entry);

            // Oferece o horário vago imediatamente para o próximo da fila
            processWaitlistSlotOfferUseCase.execute(professionalId, serviceId, offeredTime);
        }
    }
}

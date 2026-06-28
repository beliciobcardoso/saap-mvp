package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.application.service.NotificationService;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessWaitlistSlotOfferUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWaitlistSlotOfferUseCase.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AppointmentActionTokenService tokenService;
    private final NotificationService notificationService;

    @Value("${saap.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public void execute(UUID professionalId, UUID serviceId, LocalDateTime dateTime) {
        log.info("Processando fila de espera para profissional {} e serviço {} no horário {}", 
                professionalId, serviceId, dateTime);

        List<WaitlistEntry> activeEntries = waitlistEntryRepository
                .findActiveByProfessionalAndServiceOrderByCreatedAtAsc(professionalId, serviceId);

        if (activeEntries.isEmpty()) {
            log.info("Nenhum paciente ativo na fila de espera para o profissional {} e serviço {}", 
                    professionalId, serviceId);
            return;
        }

        // FIFO: obter o primeiro
        WaitlistEntry entry = activeEntries.getFirst();
        entry.offer(dateTime, 30L); // 30 minutos de validade

        waitlistEntryRepository.save(entry);

        String acceptToken = tokenService.generateToken(entry.getId(), "accept-waitlist");
        String declineToken = tokenService.generateToken(entry.getId(), "decline-waitlist");

        String acceptLink = baseUrl + "/api/v1/appointments/public/waitlist/accept?token=" + acceptToken;
        String declineLink = baseUrl + "/api/v1/appointments/public/waitlist/decline?token=" + declineToken;

        notificationService.sendWaitlistOfferNotification(entry, acceptLink, declineLink);
        log.info("Vaga oferecida com sucesso para o paciente {}", entry.getPatient().getName());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeclineWaitlistOfferUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeclineWaitlistOfferUseCase.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AppointmentActionTokenService tokenService;
    private final ProcessWaitlistSlotOfferUseCase processWaitlistSlotOfferUseCase;

    @Transactional
    public void execute(String token) {
        AppointmentActionTokenService.DecodedToken decoded = tokenService.validateToken(token);
        if (!"decline-waitlist".equals(decoded.action())) {
            throw new IllegalArgumentException("Ação inválida no token");
        }

        WaitlistEntry entry = waitlistEntryRepository.findById(decoded.appointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Entrada da fila de espera não encontrada"));

        if (!entry.isActive() || entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new IllegalStateException("Esta oferta de vaga não está mais ativa para ser recusada");
        }

        LocalDateTime offeredTime = entry.getOfferedAppointmentTime();
        UUID professionalId = entry.getProfessional().getId();
        UUID serviceId = entry.getService().getId();

        entry.decline();
        waitlistEntryRepository.save(entry);

        log.info("Paciente {} recusou a vaga oferecida. Cascateando para o próximo da fila...", entry.getPatient().getName());

        // Processa o próximo da fila imediatamente
        processWaitlistSlotOfferUseCase.execute(professionalId, serviceId, offeredTime);
    }
}

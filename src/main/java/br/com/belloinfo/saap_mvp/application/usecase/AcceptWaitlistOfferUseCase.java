package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AcceptWaitlistOfferUseCase {

    private static final Logger log = LoggerFactory.getLogger(AcceptWaitlistOfferUseCase.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentActionTokenService tokenService;
    private final BookAppointmentUseCase bookAppointmentUseCase;

    @Transactional
    public Appointment execute(String token) {
        AppointmentActionTokenService.DecodedToken decoded = tokenService.validateToken(token);
        if (!"accept-waitlist".equals(decoded.action())) {
            throw new IllegalArgumentException("Ação inválida no token");
        }

        WaitlistEntry entry = waitlistEntryRepository.findById(decoded.appointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Entrada da fila de espera não encontrada"));

        if (!entry.isActive() || entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new IllegalStateException("Esta oferta de vaga não está mais ativa");
        }

        if (entry.getOfferExpiresAt().isBefore(LocalDateTime.now())) {
            entry.expire();
            waitlistEntryRepository.save(entry);
            throw new IllegalStateException("O prazo para aceitar esta vaga expirou");
        }

        try {
            // Tenta reservar o horário usando as regras de validação padrão (inclui lock pessimista)
            Appointment appointment = bookAppointmentUseCase.execute(
                    entry.getPatient().getId(),
                    entry.getProfessional().getId(),
                    entry.getService().getId(),
                    entry.getOfferedAppointmentTime(),
                    PaymentMethod.PIX, // Padrão para alocação da fila
                    PriorityLevel.P5
            );

            // Transiciona o status do agendamento para CONFIRMED conforme a especificação da vaga aceita
            appointment.transitionTo(AppointmentStatus.CONFIRMED);
            Appointment confirmedAppointment = appointmentRepository.save(appointment);

            entry.accept();
            waitlistEntryRepository.save(entry);

            log.info("Vaga aceita e consulta confirmada para o agendamento {}", confirmedAppointment.getId());
            return confirmedAppointment;

        } catch (IllegalStateException e) {
            // Em caso de colisão (ex: recepcionista agendou manualmente enquanto o paciente respondia)
            log.warn("Colisão de agendamento detectada ao aceitar vaga. Retornando paciente para a fila: {}", e.getMessage());
            
            // Retorna o paciente para o estado WAITING para não perder a sua posição FIFO na fila
            entry.setStatus(WaitlistStatus.WAITING);
            entry.setOfferedAppointmentTime(null);
            entry.setOfferExpiresAt(null);
            waitlistEntryRepository.save(entry);

            throw new IllegalStateException("A vaga já foi preenchida por outro agendamento. Você continuará na fila de espera para as próximas vagas.");
        }
    }
}

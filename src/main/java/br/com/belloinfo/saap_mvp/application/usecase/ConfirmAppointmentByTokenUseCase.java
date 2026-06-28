package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Confirma um agendamento via token JWT público (link do e-mail de follow-up).
 * Valida o token, verifica que o agendamento está em PENDING_RESPONSE e transiciona para CONFIRMED.
 */
@Component
@RequiredArgsConstructor
public class ConfirmAppointmentByTokenUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentActionTokenService tokenService;

    @Transactional
    public Appointment execute(String token) {
        AppointmentActionTokenService.DecodedToken decoded = tokenService.validateToken(token);
        if (!"confirm".equals(decoded.action())) {
            throw new IllegalArgumentException("Token de ação inválido para confirmação");
        }

        UUID appointmentId = decoded.appointmentId();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.PENDING_RESPONSE) {
            throw new IllegalStateException(
                "Não é possível confirmar agendamento com status: " + appointment.getStatus());
        }

        appointment.transitionTo(AppointmentStatus.CONFIRMED);
        return appointmentRepository.save(appointment);
    }
}

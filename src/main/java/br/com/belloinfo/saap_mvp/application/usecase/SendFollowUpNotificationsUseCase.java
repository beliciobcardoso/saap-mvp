package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.application.service.NotificationService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SendFollowUpNotificationsUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentActionTokenService tokenService;
    private final NotificationService notificationService;

    @Transactional
    public void execute(String baseUrl) {
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime tomorrowEnd = LocalDate.now().plusDays(1).atTime(LocalTime.MAX);

        List<Appointment> pendingAppointments = appointmentRepository.findByStatusAndDateTimeBetweenAndFollowUpSentFalse(
                AppointmentStatus.PENDING,
                tomorrowStart,
                tomorrowEnd
        );

        for (Appointment appointment : pendingAppointments) {
            String confirmToken = tokenService.generateToken(appointment.getId(), "confirm");
            String cancelToken = tokenService.generateToken(appointment.getId(), "cancel");

            String confirmLink = baseUrl + "/api/v1/appointments/public/confirm?token=" + confirmToken;
            String cancelLink = baseUrl + "/api/v1/appointments/public/cancel?token=" + cancelToken;

            notificationService.sendFollowUpNotification(appointment, confirmLink, cancelLink);

            appointment.setFollowUpSent(true);
            appointmentRepository.save(appointment);
        }
    }
}

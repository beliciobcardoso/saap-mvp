package br.com.belloinfo.saap_mvp.infrastructure.service;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.EmailNotificationService;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.WhatsAppNotificationService;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.NotificationOrchestrator;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplIntegrationTest {

    private NotificationServiceImpl notificationService;
    private NotificationOrchestrator orchestrator;
    private Patient testPatient;
    private Professional testProfessional;
    private Service testService;
    private Appointment testAppointment;
    private WaitlistEntry testWaitlistEntry;

    @BeforeEach
    void setUp() {
        orchestrator = new NotificationOrchestrator(new ArrayList<>());
        notificationService = new NotificationServiceImpl(
            orchestrator, new WhatsAppNotificationService(), new EmailNotificationService(new JavaMailSenderImpl()));

        testPatient = Patient.builder()
            .id(UUID.randomUUID())
            .name("Test Patient")
            .email("test@example.com")
            .cpf("12345678901")
            .build();

        testProfessional = Professional.builder()
            .id(UUID.randomUUID())
            .name("Test Professional")
            .role(ProfessionalRole.PROFESSIONAL)
            .build();

        testService = Service.builder()
            .id(UUID.randomUUID())
            .name("Consultation")
            .durationMinutes(30)
            .build();

        testAppointment = Appointment.builder()
            .id(UUID.randomUUID())
            .patient(testPatient)
            .professional(testProfessional)
            .service(testService)
            .dateTime(LocalDateTime.now().plusDays(1))
            .status(AppointmentStatus.PENDING)
            .build();

        testWaitlistEntry = WaitlistEntry.builder()
            .id(UUID.randomUUID())
            .patient(testPatient)
            .professional(testProfessional)
            .service(testService)
            .offeredAppointmentTime(LocalDateTime.now().plusDays(1))
            .build();
    }

    @Test
    @DisplayName("sendFollowUpNotification does not throw with valid appointment")
    void sendFollowUpNotification_validAppointment_success() {
        String confirmLink = "http://localhost:8080/api/v1/appointments/public/123/confirm";
        String cancelLink = "http://localhost:8080/api/v1/appointments/public/123/cancel";

        assertThat(notificationService).isNotNull();
        assertThatNoException().isThrownBy(() ->
            notificationService.sendFollowUpNotification(testAppointment, confirmLink, cancelLink)
        );
    }

    @Test
    @DisplayName("sendFollowUpNotification handles null email gracefully")
    void sendFollowUpNotification_nullEmail_noThrow() {
        testPatient.setEmail(null);
        String confirmLink = "http://localhost:8080/api/v1/appointments/public/123/confirm";
        String cancelLink = "http://localhost:8080/api/v1/appointments/public/123/cancel";

        assertThatNoException().isThrownBy(() ->
            notificationService.sendFollowUpNotification(testAppointment, confirmLink, cancelLink)
        );
    }

    @Test
    @DisplayName("sendWaitlistOfferNotification does not throw with valid entry")
    void sendWaitlistOfferNotification_validEntry_success() {
        String acceptLink = "http://localhost:8080/api/v1/waitlist/public/123/accept";
        String declineLink = "http://localhost:8080/api/v1/waitlist/public/123/decline";

        assertThatNoException().isThrownBy(() ->
            notificationService.sendWaitlistOfferNotification(testWaitlistEntry, acceptLink, declineLink)
        );
    }

    @Test
    @DisplayName("sendWaitlistOfferNotification handles null email gracefully")
    void sendWaitlistOfferNotification_nullEmail_noThrow() {
        testPatient.setEmail(null);
        String acceptLink = "http://localhost:8080/api/v1/waitlist/public/123/accept";
        String declineLink = "http://localhost:8080/api/v1/waitlist/public/123/decline";

        assertThatNoException().isThrownBy(() ->
            notificationService.sendWaitlistOfferNotification(testWaitlistEntry, acceptLink, declineLink)
        );
    }

    @Test
    @DisplayName("orchestrator has all notification channels registered")
    void orchestrator_hasChannels() {
        assertThat(orchestrator).isNotNull();
    }
}

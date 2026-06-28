package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class SendFollowUpNotificationsUseCaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TriggerFollowUpUseCase triggerFollowUpUseCase;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    private Patient patient;
    private Professional professional;
    private Service service;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_notif_" + uniqueSuffix + "@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Notif " + uniqueSuffix)
                .email("notif_" + uniqueSuffix + "@saap.com")
                .phone("11988887744")
                .registrationNumber("CRM-NOTIF-" + uniqueSuffix)
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Notif Patient " + uniqueSuffix)
                .cpf(generateUniqueCpf())
                .phone("11977776644")
                .birthDate(LocalDate.of(1990, 7, 10))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Follow-Up " + uniqueSuffix)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(service);
    }

    private String generateUniqueCpf() {
        // Generate an 11-digit numeric string unique per test
        return String.format("%011d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000_000L));
    }

    /**
     * Agendamento PENDING dentro da janela (amanhã) → deve ser notificado e transicionar para PENDING_RESPONSE.
     * Agendamento CONFIRMED (dentro da janela) → deve ser ignorado.
     * Agendamento PENDING além da janela (3 dias) → deve ser ignorado (janela padrão de 48h).
     */
    @Test
    void shouldProcessOnlyEligiblePendingAppointmentsWithinWindow() {
        LocalDateTime tomorrow = LocalDateTime.now().plusHours(24).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime beyondWindow = LocalDateTime.now().plusDays(4).withHour(12).withMinute(0).withSecond(0).withNano(0);

        // Eligible: PENDING, within 48h window, no prior notification
        Appointment eligible = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(tomorrow)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(eligible);

        // Ineligible: CONFIRMED status
        Appointment confirmed = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(tomorrow.plusHours(2))
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(confirmed);

        // Ineligible: PENDING but beyond window (4 days)
        Appointment beyondWindowApp = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(beyondWindow)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(beyondWindowApp);

        // Execute
        triggerFollowUpUseCase.execute("http://localhost:8080");

        // Verify: eligible → PENDING_RESPONSE, followUpSent=true, followUpSentAt set
        Appointment resultEligible = appointmentRepository.findById(eligible.getId()).orElseThrow();
        assertEquals(AppointmentStatus.PENDING_RESPONSE, resultEligible.getStatus());
        assertTrue(resultEligible.isFollowUpSent());
        assertNotNull(resultEligible.getFollowUpSentAt());

        // Verify: confirmed → unchanged
        Appointment resultConfirmed = appointmentRepository.findById(confirmed.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, resultConfirmed.getStatus());
        assertFalse(resultConfirmed.isFollowUpSent());

        // Verify: beyond window → unchanged
        Appointment resultBeyond = appointmentRepository.findById(beyondWindowApp.getId()).orElseThrow();
        assertEquals(AppointmentStatus.PENDING, resultBeyond.getStatus());
        assertFalse(resultBeyond.isFollowUpSent());
    }

    /** Agendamento já notificado (followUpSentAt não-nulo) não deve ser reprocessado. */
    @Test
    void shouldNotResendNotificationForAlreadyNotifiedAppointment() {
        LocalDateTime tomorrow = LocalDateTime.now().plusHours(24).withMinute(0).withSecond(0).withNano(0);

        Appointment alreadyNotified = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(tomorrow)
                .status(AppointmentStatus.PENDING_RESPONSE)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .followUpSent(true)
                .followUpSentAt(LocalDateTime.now().minusHours(2))
                .build();
        appointmentRepository.save(alreadyNotified);

        triggerFollowUpUseCase.execute("http://localhost:8080");

        // Status should remain PENDING_RESPONSE (not reset)
        Appointment result = appointmentRepository.findById(alreadyNotified.getId()).orElseThrow();
        assertEquals(AppointmentStatus.PENDING_RESPONSE, result.getStatus());
    }
}

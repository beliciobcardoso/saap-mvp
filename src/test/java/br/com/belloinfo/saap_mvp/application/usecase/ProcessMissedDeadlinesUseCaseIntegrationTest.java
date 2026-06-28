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
import br.com.belloinfo.saap_mvp.infrastructure.config.ClinicSettings;
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
class ProcessMissedDeadlinesUseCaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProcessMissedDeadlinesUseCase processMissedDeadlinesUseCase;

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

    @Autowired
    private ClinicSettings clinicSettings;

    private Patient patient;
    private Professional professional;
    private Service service;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_dl_" + uniqueSuffix + "@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Deadline " + uniqueSuffix)
                .email("deadline_" + uniqueSuffix + "@saap.com")
                .phone("11988887755")
                .registrationNumber("CRM-DL-" + uniqueSuffix)
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Deadline Patient " + uniqueSuffix)
                .cpf(String.format("%011d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000_000L)))
                .phone("11977776655")
                .birthDate(LocalDate.of(1985, 9, 25))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Deadline " + uniqueSuffix)
                .durationMinutes(45)
                .price(BigDecimal.valueOf(200.00))
                .active(true)
                .build();
        serviceRepository.save(service);
    }

    /**
     * Com autoCancelAfterNoResponse=true (default), agendamentos em PENDING_RESPONSE
     * cujo dateTime <= now + deadlineHours devem ser cancelados automaticamente.
     */
    @Test
    void shouldAutoCancelAppointmentWhenDeadlineExpiredAndAutoCancelEnabled() {
        assertTrue(clinicSettings.isAutoCancelAfterNoResponse(),
                "autoCancelAfterNoResponse deve ser true no perfil de teste");

        // Past deadline: appointment is in 2h (within 24h deadline window)
        Appointment expiredApp = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now().plusHours(2))
                .status(AppointmentStatus.PENDING_RESPONSE)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .followUpSent(true)
                .followUpSentAt(LocalDateTime.now().minusHours(22))
                .build();
        appointmentRepository.save(expiredApp);

        // Not expired: appointment is in 30h (beyond 24h deadline)
        Appointment safeApp = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now().plusHours(30))
                .status(AppointmentStatus.PENDING_RESPONSE)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .followUpSent(true)
                .followUpSentAt(LocalDateTime.now().minusHours(18))
                .build();
        appointmentRepository.save(safeApp);

        processMissedDeadlinesUseCase.execute();

        Appointment resultExpired = appointmentRepository.findById(expiredApp.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, resultExpired.getStatus());

        Appointment resultSafe = appointmentRepository.findById(safeApp.getId()).orElseThrow();
        assertEquals(AppointmentStatus.PENDING_RESPONSE, resultSafe.getStatus());
    }

    /**
     * Com autoCancelAfterNoResponse=false, agendamentos expirados devem ser marcados
     * como followUpRequired=true (sem cancelamento automático).
     */
    @Test
    void shouldMarkFollowUpRequiredWhenAutoCancelDisabled() {
        // Temporarily disable auto-cancel
        clinicSettings.setAutoCancelAfterNoResponse(false);

        Appointment expiredApp = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now().plusHours(2))
                .status(AppointmentStatus.PENDING_RESPONSE)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .followUpSent(true)
                .followUpSentAt(LocalDateTime.now().minusHours(22))
                .build();
        appointmentRepository.save(expiredApp);

        processMissedDeadlinesUseCase.execute();

        Appointment result = appointmentRepository.findById(expiredApp.getId()).orElseThrow();
        // Status must remain PENDING_RESPONSE (no auto-cancel)
        assertEquals(AppointmentStatus.PENDING_RESPONSE, result.getStatus());
        // But followUpRequired must be flagged for manual reception action
        assertTrue(result.isFollowUpRequired());
    }
}

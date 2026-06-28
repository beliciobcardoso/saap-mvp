package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.*;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.*;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaAppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class PriorityAttendanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CheckInAppointmentUseCase checkInAppointmentUseCase;

    @Autowired
    private CallNextPatientUseCase callNextPatientUseCase;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JpaAppointmentRepository jpaAppointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Patient patient1;
    private Patient patient2;
    private Professional professional;
    private Service service;
    private User receptionistUser;
    private User professionalUser;

    @BeforeEach
    void setUp() {
        // Clear database tables to ensure clean state
        jpaAppointmentRepository.deleteAll();

        receptionistUser = User.builder()
                .id(UUID.randomUUID())
                .email("receptionist_priority@saap.com")
                .password("pwd")
                .role(UserRole.RECEPTIONIST)
                .active(true)
                .build();
        userRepository.save(receptionistUser);

        professionalUser = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_priority@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(professionalUser);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Julio")
                .email("doctor_priority@saap.com")
                .phone("11988887777")
                .registrationNumber("CRM-88888")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(professionalUser.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient1 = Patient.builder()
                .id(UUID.randomUUID())
                .name("Bruno Souza")
                .cpf("98765432100")
                .phone("11977776666")
                .birthDate(LocalDate.of(1995, 3, 15))
                .active(true)
                .build();
        patientRepository.save(patient1);

        patient2 = Patient.builder()
                .id(UUID.randomUUID())
                .name("Carlos Alberto")
                .cpf("98765432101")
                .phone("11966665555")
                .birthDate(LocalDate.of(1990, 5, 20))
                .active(true)
                .build();
        patientRepository.save(patient2);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Prioridade Legal")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(service);
    }

    @Test
    void shouldCheckInWithPositivePriorityValidation() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P1)
                .build();
        appointmentRepository.save(appointment);

        Appointment checkedIn = checkInAppointmentUseCase.execute(
                appointment.getId(),
                PriorityLevel.P1,
                receptionistUser.getId(),
                "Laudo de TEA Apresentado",
                "192.168.0.1"
        );

        assertNotNull(checkedIn);
        assertEquals(AppointmentStatus.ARRIVED, checkedIn.getStatus());
        assertEquals(PriorityLevel.P1, checkedIn.getPriorityLevel());
        assertNotNull(checkedIn.getPriorityScore());
        assertEquals("Laudo de TEA Apresentado", checkedIn.getPriorityNotes());
        assertEquals(receptionistUser.getId(), checkedIn.getPriorityVerifiedBy());

        // Assert Audit Log was persisted
        List<AuditLog> logs = auditLogRepository.findAll();
        assertFalse(logs.isEmpty());
        AuditLog log = logs.stream().filter(l -> l.getAppointmentId().equals(appointment.getId())).findFirst().orElse(null);
        assertNotNull(log);
        assertEquals("CHECK_IN_VALIDACAO_PRIORIDADE", log.getAction());
        assertEquals(receptionistUser.getId(), log.getUserId());
        assertEquals("192.168.0.1", log.getIpAddress());
    }

    @Test
    void shouldCheckInWithNegativePriorityReversionToP5() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P1)
                .build();
        appointmentRepository.save(appointment);

        Appointment checkedIn = checkInAppointmentUseCase.execute(
                appointment.getId(),
                PriorityLevel.P5, // Reverted to normal by receptionist
                receptionistUser.getId(),
                "Ausência de laudo ou comprovante",
                "192.168.0.1"
        );

        assertNotNull(checkedIn);
        assertEquals(AppointmentStatus.ARRIVED, checkedIn.getStatus());
        assertEquals(PriorityLevel.P5, checkedIn.getPriorityLevel());
        assertEquals("Ausência de laudo ou comprovante", checkedIn.getPriorityNotes());
        assertEquals(receptionistUser.getId(), checkedIn.getPriorityVerifiedBy());

        // Assert Audit Log
        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog log = logs.stream().filter(l -> l.getAppointmentId().equals(appointment.getId())).findFirst().orElse(null);
        assertNotNull(log);
        assertEquals("CHECK_IN_VALIDACAO_PRIORIDADE", log.getAction());
    }

    @Test
    void shouldCallNextPatientBasedOnPriorityScoreOrdering() throws InterruptedException {
        // App 1: P2 priority, checked-in first
        Appointment app1 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P2)
                .build();
        appointmentRepository.save(app1);

        // App 2: P1 priority (higher priority), checked-in slightly later
        Appointment app2 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient2)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P1)
                .build();
        appointmentRepository.save(app2);

        checkInAppointmentUseCase.execute(app1.getId(), PriorityLevel.P2, receptionistUser.getId(), "Notes", "127.0.0.1");
        Thread.sleep(10); // slight delay to guarantee later timestamp for app2
        checkInAppointmentUseCase.execute(app2.getId(), PriorityLevel.P1, receptionistUser.getId(), "Notes", "127.0.0.1");

        // The professional calls next patient. Since app2 is P1, it should be called first despite checking-in later.
        Appointment called = callNextPatientUseCase.execute(professional.getId(), professionalUser.getId(), "192.168.0.2");
        assertNotNull(called);
        assertEquals(app2.getId(), called.getId());
        assertEquals(AppointmentStatus.CALLING, called.getStatus());

        // Next call should call app1 (P2)
        Appointment calledSecond = callNextPatientUseCase.execute(professional.getId(), professionalUser.getId(), "192.168.0.2");
        assertNotNull(calledSecond);
        assertEquals(app1.getId(), calledSecond.getId());
        assertEquals(AppointmentStatus.CALLING, calledSecond.getStatus());

        // Next call should fail because queue is empty
        assertThrows(IllegalStateException.class, () -> {
            callNextPatientUseCase.execute(professional.getId(), professionalUser.getId(), "192.168.0.2");
        });
    }

    @Test
    void shouldCallNextPatientFIFOForSamePriority() throws InterruptedException {
        // App 1: P1 priority, checked-in first
        Appointment app1 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P1)
                .build();
        appointmentRepository.save(app1);

        // App 2: P1 priority, checked-in later
        Appointment app2 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient2)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P1)
                .build();
        appointmentRepository.save(app2);

        checkInAppointmentUseCase.execute(app1.getId(), PriorityLevel.P1, receptionistUser.getId(), "Notes", "127.0.0.1");
        Thread.sleep(10); // delay
        checkInAppointmentUseCase.execute(app2.getId(), PriorityLevel.P1, receptionistUser.getId(), "Notes", "127.0.0.1");

        // The professional calls next patient. Since both are P1, app1 (checked-in first) should be called.
        Appointment called = callNextPatientUseCase.execute(professional.getId(), professionalUser.getId(), "192.168.0.2");
        assertNotNull(called);
        assertEquals(app1.getId(), called.getId());
    }
}

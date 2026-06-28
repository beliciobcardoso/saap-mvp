package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import br.com.belloinfo.saap_mvp.infrastructure.scheduler.WaitlistTimeoutScheduler;
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
class WaitlistIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CancelAppointmentUseCase cancelUseCase;

    @Autowired
    private AcceptWaitlistOfferUseCase acceptUseCase;

    @Autowired
    private DeclineWaitlistOfferUseCase declineUseCase;

    @Autowired
    private WaitlistTimeoutScheduler timeoutScheduler;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

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
    private AppointmentActionTokenService tokenService;

    private Patient patient1;
    private Patient patient2;
    private Professional professional;
    private Service service;
    private User user;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_wait@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Wait")
                .email("wait@saap.com")
                .phone("11988887777")
                .registrationNumber("CRM-44444")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient1 = Patient.builder()
                .id(UUID.randomUUID())
                .name("Wait Patient 1")
                .cpf("55566677788")
                .phone("11977776666")
                .birthDate(LocalDate.of(1995, 3, 15))
                .active(true)
                .build();
        patientRepository.save(patient1);

        patient2 = Patient.builder()
                .id(UUID.randomUUID())
                .name("Wait Patient 2")
                .cpf("66677788899")
                .phone("11977775555")
                .birthDate(LocalDate.of(1997, 4, 16))
                .active(true)
                .build();
        patientRepository.save(patient2);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Geral Fila")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(service);

        appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now().plusDays(2))
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(appointment);
    }

    @Test
    void shouldOfferSlotToFirstWaitlistEntryOnCancelAndDeclineAndAcceptCascade() {
        // 1. Cadastra dois pacientes na fila de espera (FIFO order)
        WaitlistEntry entry1 = WaitlistEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .status(WaitlistStatus.WAITING)
                .active(true)
                .createdAt(LocalDateTime.now().minusHours(2)) // Mais antigo
                .build();
        waitlistEntryRepository.save(entry1);

        WaitlistEntry entry2 = WaitlistEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient2)
                .professional(professional)
                .service(service)
                .status(WaitlistStatus.WAITING)
                .active(true)
                .createdAt(LocalDateTime.now().minusHours(1)) // Mais novo
                .build();
        waitlistEntryRepository.save(entry2);

        // 2. Cancela a consulta -> deve acionar oferta para entry1
        LocalDateTime slotTime = appointment.getDateTime();
        cancelUseCase.execute(appointment.getId());

        WaitlistEntry result1 = waitlistEntryRepository.findById(entry1.getId()).orElseThrow();
        WaitlistEntry result2 = waitlistEntryRepository.findById(entry2.getId()).orElseThrow();

        assertEquals(WaitlistStatus.OFFERED, result1.getStatus());
        assertEquals(slotTime, result1.getOfferedAppointmentTime());
        assertNotNull(result1.getOfferExpiresAt());
        assertTrue(result1.isActive());

        assertEquals(WaitlistStatus.WAITING, result2.getStatus());
        assertNull(result2.getOfferedAppointmentTime());

        // 3. Paciente 1 recusa a vaga -> deve marcar entry1 como DECLINED e cascatear para entry2
        String declineToken = tokenService.generateToken(entry1.getId(), "decline-waitlist");
        declineUseCase.execute(declineToken);

        result1 = waitlistEntryRepository.findById(entry1.getId()).orElseThrow();
        result2 = waitlistEntryRepository.findById(entry2.getId()).orElseThrow();

        assertEquals(WaitlistStatus.DECLINED, result1.getStatus());
        assertFalse(result1.isActive());

        assertEquals(WaitlistStatus.OFFERED, result2.getStatus());
        assertEquals(slotTime, result2.getOfferedAppointmentTime());
        assertTrue(result2.isActive());

        // 4. Paciente 2 aceita a vaga -> deve marcar entry2 como ACCEPTED e criar a nova consulta CONFIRMED
        String acceptToken = tokenService.generateToken(entry2.getId(), "accept-waitlist");
        Appointment newAppointment = acceptUseCase.execute(acceptToken);

        result2 = waitlistEntryRepository.findById(entry2.getId()).orElseThrow();

        assertEquals(WaitlistStatus.ACCEPTED, result2.getStatus());
        assertFalse(result2.isActive());

        assertNotNull(newAppointment);
        assertEquals(patient2.getId(), newAppointment.getPatient().getId());
        assertEquals(slotTime, newAppointment.getDateTime());
        assertEquals(AppointmentStatus.CONFIRMED, newAppointment.getStatus());
    }

    @Test
    void shouldExpireOfferAndCascadeToNextOnSchedulerRun() {
        // 1. Cadastra dois pacientes na fila de espera
        WaitlistEntry entry1 = WaitlistEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient1)
                .professional(professional)
                .service(service)
                .status(WaitlistStatus.OFFERED) // Já oferecido
                .offeredAppointmentTime(appointment.getDateTime())
                .offerExpiresAt(LocalDateTime.now().minusMinutes(5)) // Expirado há 5 min
                .active(true)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        waitlistEntryRepository.save(entry1);

        WaitlistEntry entry2 = WaitlistEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient2)
                .professional(professional)
                .service(service)
                .status(WaitlistStatus.WAITING)
                .active(true)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        waitlistEntryRepository.save(entry2);

        // 2. Executa o scheduler de timeout -> deve marcar entry1 como EXPIRED e oferecer para entry2
        timeoutScheduler.runWaitlistTimeoutJob();

        WaitlistEntry result1 = waitlistEntryRepository.findById(entry1.getId()).orElseThrow();
        WaitlistEntry result2 = waitlistEntryRepository.findById(entry2.getId()).orElseThrow();

        assertEquals(WaitlistStatus.EXPIRED, result1.getStatus());
        assertFalse(result1.isActive());

        assertEquals(WaitlistStatus.OFFERED, result2.getStatus());
        assertEquals(appointment.getDateTime(), result2.getOfferedAppointmentTime());
        assertTrue(result2.isActive());
    }
}

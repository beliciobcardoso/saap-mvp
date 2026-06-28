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
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaAppointmentRepository;
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
    private SendFollowUpNotificationsUseCase useCase;

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

    private Patient patient;
    private Professional professional;
    private Service service;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_notif@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Notif")
                .email("notif@saap.com")
                .phone("11988887777")
                .registrationNumber("CRM-66666")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Notif Patient")
                .cpf("33344455566")
                .phone("11977776666")
                .birthDate(LocalDate.of(1995, 3, 15))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(service);
    }

    @Test
    void shouldProcessOnlyEligibleTomorrowPendingAppointments() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime future = LocalDateTime.now().plusDays(3).withHour(12).withMinute(0).withSecond(0).withNano(0);

        // App 1: Pending, tomorrow (Eligible)
        Appointment appTomorrowPending = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(tomorrow)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(appTomorrowPending);

        // App 2: Confirmed, tomorrow (Ineligible status)
        Appointment appTomorrowConfirmed = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(tomorrow.plusHours(2)) // diferente horario
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(appTomorrowConfirmed);

        // App 3: Pending, future date (Ineligible date)
        Appointment appFuturePending = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(future)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(appFuturePending);

        // Execute Use Case
        useCase.execute("http://localhost:8080");

        // Verify state updates
        Appointment resultTomorrowPending = appointmentRepository.findById(appTomorrowPending.getId()).orElseThrow();
        Appointment resultTomorrowConfirmed = appointmentRepository.findById(appTomorrowConfirmed.getId()).orElseThrow();
        Appointment resultFuturePending = appointmentRepository.findById(appFuturePending.getId()).orElseThrow();

        assertTrue(resultTomorrowPending.isFollowUpSent());
        assertFalse(resultTomorrowConfirmed.isFollowUpSent());
        assertFalse(resultFuturePending.isFollowUpSent());
    }
}

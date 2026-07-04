package br.com.belloinfo.saap_mvp.infrastructure.persistence;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.PageResult;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class AppointmentRepositoryIntegrationTest extends BaseIntegrationTest {

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
        // Setup dependencies
        user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_int@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Julio")
                .email("julio@saap.com")
                .phone("11988887777")
                .registrationNumber("CRM-88888")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Bruno Souza")
                .cpf("11122233344")
                .phone("11977776666")
                .birthDate(LocalDate.of(1995, 3, 15))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Neurologia")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(350.00))
                .active(true)
                .build();
        serviceRepository.save(service);
    }

    @Test
    void shouldSaveAndFindAppointment() {
        LocalDateTime time = LocalDateTime.now().plusDays(2).withNano(0);
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(time)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        assertNotNull(saved);
        assertNotNull(saved.getId());

        Optional<Appointment> found = appointmentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(patient.getId(), found.get().getPatient().getId());
        assertEquals(professional.getId(), found.get().getProfessional().getId());
        assertEquals(service.getId(), found.get().getService().getId());
        assertEquals(time, found.get().getDateTime());
        assertEquals(AppointmentStatus.PENDING, found.get().getStatus());
        assertEquals(PaymentMethod.PIX, found.get().getPaymentMethod());
    }

    @Test
    void shouldVerifyActiveAppointmentExists() {
        LocalDateTime time = LocalDateTime.now().plusDays(3).withNano(0);
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(time)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();

        appointmentRepository.save(appointment);

        boolean exists = appointmentRepository.existsByProfessionalIdAndDateTimeAndStatusNotIn(
                professional.getId(),
                time,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        assertTrue(exists);

        boolean notExists = appointmentRepository.existsByProfessionalIdAndDateTimeAndStatusNotIn(
                professional.getId(),
                time.plusHours(1),
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        assertFalse(notExists);
    }

    @Test
    void shouldFailToSaveDoubleBookingDueToUniqueConstraint() {
        LocalDateTime time = LocalDateTime.now().plusDays(4).withNano(0);
        Appointment app1 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(time)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(app1);

        Appointment app2 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(time) // Mesmo profissional e horário
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.CASH)
                .priorityLevel(PriorityLevel.P5)
                .build();

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            appointmentRepository.save(app2);
            jpaAppointmentRepository.flush(); // Força o banco de dados a validar
        });
    }

    @Test
    void shouldFilterAppointmentsCorrectly() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(5).withNano(0);

        Appointment app1 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(baseTime)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(app1);

        Appointment app2 = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(baseTime.plusDays(1))
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(app2);

        // Filter by professional
        PageResult<Appointment> filtered = appointmentRepository.findByFilters(
                professional.getId(),
                null,
                baseTime.minusHours(1),
                baseTime.plusHours(1),
                0, 20
        );
        assertEquals(1, filtered.content().size());
        assertEquals(app1.getId(), filtered.content().get(0).getId());

        // Filter by patient
        PageResult<Appointment> filtered2 = appointmentRepository.findByFilters(
                null,
                patient.getId(),
                baseTime.minusHours(1),
                baseTime.plusDays(2),
                0, 20
        );
        assertEquals(2, filtered2.content().size());
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AppointmentPublicControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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

    private Patient patient;
    private Professional professional;
    private Service service;
    private User user;
    private Appointment pendingResponseAppointment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_public_v2@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Public v2")
                .email("public_v2@saap.com")
                .phone("11988887722")
                .registrationNumber("CRM-44444")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Public Patient v2")
                .cpf("55566677788")
                .phone("11977776622")
                .birthDate(LocalDate.of(1992, 5, 20))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Follow-up")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(180.00))
                .active(true)
                .build();
        serviceRepository.save(service);

        // Appointment in PENDING_RESPONSE (notification already sent, awaiting patient response)
        pendingResponseAppointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.PENDING_RESPONSE)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .followUpSent(true)
                .followUpSentAt(LocalDateTime.now().minusHours(1))
                .build();
        appointmentRepository.save(pendingResponseAppointment);
    }

    @Test
    void shouldConfirmPendingResponseAppointmentPubliclyWithoutAuth() throws Exception {
        String token = tokenService.generateToken(pendingResponseAppointment.getId(), "confirm");

        mockMvc.perform(get("/api/v1/appointments/public/confirm")
                        .param("token", token))
                .andExpect(status().isOk());

        Appointment updated = appointmentRepository.findById(pendingResponseAppointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, updated.getStatus());
    }

    @Test
    void shouldCancelPendingResponseAppointmentPubliclyWithoutAuth() throws Exception {
        String token = tokenService.generateToken(pendingResponseAppointment.getId(), "cancel");

        mockMvc.perform(get("/api/v1/appointments/public/cancel")
                        .param("token", token))
                .andExpect(status().isOk());

        Appointment updated = appointmentRepository.findById(pendingResponseAppointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void shouldReturnBadRequestWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/public/confirm")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenAppointmentIsNotInPendingResponseStatus() throws Exception {
        // Create a CONFIRMED appointment (already past the follow-up flow)
        Appointment confirmedAppointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now().plusDays(2))
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(confirmedAppointment);

        String token = tokenService.generateToken(confirmedAppointment.getId(), "confirm");

        mockMvc.perform(get("/api/v1/appointments/public/confirm")
                        .param("token", token))
                .andExpect(status().isConflict());
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

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
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class WaitlistPublicControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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

    private Patient patient;
    private Professional professional;
    private Service service;
    private User user;
    private WaitlistEntry waitlistEntry;
    private LocalDateTime offerTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_ctl@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Ctl")
                .email("ctl@saap.com")
                .phone("11988887777")
                .registrationNumber("CRM-33333")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Ctl Patient")
                .cpf("77788899900")
                .phone("11977776666")
                .birthDate(LocalDate.of(1995, 3, 15))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Geral Ctl")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(service);

        offerTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        waitlistEntry = WaitlistEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .status(WaitlistStatus.OFFERED)
                .offeredAppointmentTime(offerTime)
                .offerExpiresAt(LocalDateTime.now().plusHours(1))
                .active(true)
                .build();
        waitlistEntryRepository.save(waitlistEntry);
    }

    @Test
    void shouldAcceptWaitlistOfferPubliclyWithoutAuth() throws Exception {
        String token = tokenService.generateToken(waitlistEntry.getId(), "accept-waitlist");

        mockMvc.perform(get("/api/v1/appointments/public/waitlist/accept")
                        .param("token", token))
                .andExpect(status().isOk());

        WaitlistEntry updated = waitlistEntryRepository.findById(waitlistEntry.getId()).orElseThrow();
        assertEquals(WaitlistStatus.ACCEPTED, updated.getStatus());
        assertFalse(updated.isActive());

        List<Appointment> appointments = appointmentRepository.findAll();
        Appointment newApp = appointments.stream()
                .filter(a -> a.getPatient().getId().equals(patient.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(offerTime, newApp.getDateTime());
        assertEquals(AppointmentStatus.CONFIRMED, newApp.getStatus());
    }

    @Test
    void shouldDeclineWaitlistOfferPubliclyWithoutAuth() throws Exception {
        String token = tokenService.generateToken(waitlistEntry.getId(), "decline-waitlist");

        mockMvc.perform(get("/api/v1/appointments/public/waitlist/decline")
                        .param("token", token))
                .andExpect(status().isOk());

        WaitlistEntry updated = waitlistEntryRepository.findById(waitlistEntry.getId()).orElseThrow();
        assertEquals(WaitlistStatus.DECLINED, updated.getStatus());
        assertFalse(updated.isActive());
    }

    @Test
    void shouldReturnBadRequestWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/public/waitlist/accept")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest());
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordEntryRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordRepository;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.CreateMedicalRecordEntryRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.UpdateMedicalRecordEntryRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class MedicalRecordControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private MedicalRecordEntryRepository medicalRecordEntryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /** Sincroniza o contexto de persistência — simula fronteira de request na transação única do teste. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Patient patient;
    private Professional professional;
    private Appointment appointment;

    private static final String PROFESSIONAL_EMAIL = "prof_mr@saap.com";
    private static final String OTHER_PROFESSIONAL_EMAIL = "prof_mr_other@saap.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        User professionalUser = User.builder()
                .id(UUID.randomUUID())
                .email(PROFESSIONAL_EMAIL)
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(professionalUser);

        User otherProfessionalUser = User.builder()
                .id(UUID.randomUUID())
                .email(OTHER_PROFESSIONAL_EMAIL)
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(otherProfessionalUser);

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("admin_mr@saap.com")
                .password("pwd")
                .role(UserRole.ADMIN)
                .active(true)
                .build());
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("receptionist_mr@saap.com")
                .password("pwd")
                .role(UserRole.RECEPTIONIST)
                .active(true)
                .build());

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Prontuário")
                .email("dr.prontuario@saap.com")
                .phone("11988880001")
                .registrationNumber("CRM-90001")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(professionalUser.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        Professional otherProfessional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dra. Outra")
                .email("dra.outra@saap.com")
                .phone("11988880002")
                .registrationNumber("CRM-90002")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(otherProfessionalUser.getId())
                .active(true)
                .build();
        professionalRepository.save(otherProfessional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Paciente Prontuário")
                .cpf("11122233344")
                .phone("11977770001")
                .birthDate(LocalDate.of(1990, 1, 10))
                .active(true)
                .build();
        patientRepository.save(patient);

        Service service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Prontuário")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(200.00))
                .active(true)
                .build();
        serviceRepository.save(service);

        appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(LocalDateTime.now())
                .status(AppointmentStatus.IN_PROGRESS)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(appointment);
    }

    private String createEntryJson() throws Exception {
        return objectMapper.writeValueAsString(
                new CreateMedicalRecordEntryRequestDTO(appointment.getId(), "Paciente estável, sem queixas."));
    }

    // --- 7.4 API/Segurança ---

    @Test
    @DisplayName("401 sem token")
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/medical-records/patients/" + patient.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403 para ADMIN e RECEPTIONIST")
    void shouldReturnForbiddenForNonProfessionalRoles() throws Exception {
        for (UserRole role : new UserRole[]{UserRole.ADMIN, UserRole.RECEPTIONIST}) {
            String token = generateTestToken(role.name().toLowerCase() + "_mr@saap.com", role);
            mockMvc.perform(get("/api/v1/medical-records/patients/" + patient.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("403 para profissional divergente do agendamento")
    void shouldReturnForbiddenForDivergentProfessional() throws Exception {
        String token = generateTestToken(OTHER_PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("201 na criação e 200 na leitura/edição para o profissional do agendamento")
    void shouldAllowProfessionalOfAppointmentFullFlow() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        String createdBody = mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evolution").value("Paciente estável, sem queixas."))
                .andReturn().getResponse().getContentAsString();

        UUID entryId = UUID.fromString(objectMapper.readTree(createdBody).get("id").asText());

        mockMvc.perform(put("/api/v1/medical-records/entries/" + entryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMedicalRecordEntryRequestDTO("Evolução revisada."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolution").value("Evolução revisada."));

        flushAndClear();

        mockMvc.perform(get("/api/v1/medical-records/patients/" + patient.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patient.getId().toString()))
                .andExpect(jsonPath("$.entries[0].evolution").value("Evolução revisada."));
    }

    @Test
    @DisplayName("404 para paciente sem prontuário")
    void shouldReturnNotFoundForPatientWithoutRecord() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        mockMvc.perform(get("/api/v1/medical-records/patients/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("409 ao criar segunda entrada para o mesmo agendamento")
    void shouldReturnConflictOnDuplicateEntry() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("409 ao editar evolução após COMPLETED")
    void shouldReturnConflictOnEditAfterCompleted() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        String createdBody = mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID entryId = UUID.fromString(objectMapper.readTree(createdBody).get("id").asText());

        flushAndClear();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        mockMvc.perform(put("/api/v1/medical-records/entries/" + entryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMedicalRecordEntryRequestDTO("Tentativa pós-finalização"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("409 ao finalizar atendimento sem evolução preenchida")
    void shouldReturnConflictOnCompleteWithoutEvolution() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        mockMvc.perform(put("/api/v1/appointments/" + appointment.getId() + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        assertEquals(AppointmentStatus.IN_PROGRESS,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }

    // --- 7.3 Persistência: unicidade 1:1 e ordenação ---

    @Test
    @DisplayName("prontuário é 1:1 com paciente e entradas ordenadas por data decrescente")
    void shouldEnforceUniquenessAndOrdering() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isCreated());

        MedicalRecord record = medicalRecordRepository.findByPatientId(patient.getId()).orElseThrow();

        // segunda entrada em outro agendamento reutiliza o mesmo prontuário
        Appointment secondAppointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .professional(professional)
                .service(appointment.getService())
                .dateTime(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.IN_PROGRESS)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();
        appointmentRepository.save(secondAppointment);

        MedicalRecordEntry secondEntry = medicalRecordEntryRepository.save(MedicalRecordEntry.builder()
                .medicalRecordId(record.getId())
                .appointmentId(secondAppointment.getId())
                .professionalId(professional.getId())
                .evolution("Segunda evolução")
                .build());

        flushAndClear();
        MedicalRecord reloaded = medicalRecordRepository.findByPatientId(patient.getId()).orElseThrow();
        assertEquals(record.getId(), reloaded.getId());
        assertEquals(2, reloaded.getEntries().size());
        assertEquals(secondEntry.getId(), reloaded.getEntries().get(0).getId());
        assertFalse(reloaded.getEntries().get(0).getCreatedAt()
                .isBefore(reloaded.getEntries().get(1).getCreatedAt()));
    }

    // --- 7.5 Auditoria ---

    @Test
    @DisplayName("auditoria registrada em leitura e escrita de prontuário")
    void shouldAuditReadAndWriteOperations() throws Exception {
        String token = generateTestToken(PROFESSIONAL_EMAIL, UserRole.PROFESSIONAL);

        mockMvc.perform(post("/api/v1/medical-records/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEntryJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/medical-records/patients/" + patient.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        var actions = auditLogRepository.findAll().stream()
                .map(log -> log.getAction())
                .toList();
        assertTrue(actions.contains("MEDICAL_RECORD_ENTRY_CREATED"),
                "esperava auditoria MEDICAL_RECORD_ENTRY_CREATED, ações: " + actions);
        assertTrue(actions.contains("MEDICAL_RECORD_READ"),
                "esperava auditoria MEDICAL_RECORD_READ, ações: " + actions);
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.web.mapper;

import br.com.belloinfo.saap_mvp.domain.model.*;
import br.com.belloinfo.saap_mvp.domain.valueobject.*;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.belloinfo.saap_mvp.application.usecase.ListAuditLogsUseCase.AuditLogWithEmail;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WebMapperTest {

    private WebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = WebMapper.INSTANCE;
    }

    // ── User ──────────────────────────────────────────────

    @Test
    @DisplayName("toDomain(UserRequestDTO) mapeia campos corretamente")
    void toDomain_userRequest_mappsCorrectly() {
        UserRequestDTO dto = new UserRequestDTO("test@email.com", "senha123", UserRole.ADMIN);

        User domain = mapper.toDomain(dto);

        assertEquals("test@email.com", domain.getEmail());
        assertEquals("senha123", domain.getPassword());
        assertEquals(UserRole.ADMIN, domain.getRole());
    }

    @Test
    @DisplayName("toResponse(User) mapeia campos corretamente")
    void toResponse_user_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User domain = User.builder()
                .id(id).email("test@email.com").role(UserRole.PROFESSIONAL)
                .active(true).createdAt(now).updatedAt(now).build();

        UserResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals("test@email.com", dto.email());
        assertEquals(UserRole.PROFESSIONAL, dto.role());
        assertTrue(dto.active());
    }

    // ── Patient ───────────────────────────────────────────

    @Test
    @DisplayName("toDomain(PatientRequestDTO) mapeia campos corretamente")
    void toDomain_patientRequest_mappsCorrectly() {
        PatientRequestDTO dto = new PatientRequestDTO(
                "Maria Silva", "12345678901", "1234567890",
                "maria@email.com", "11999998888", LocalDate.of(1990, 5, 15));

        Patient domain = mapper.toDomain(dto);

        assertEquals("Maria Silva", domain.getName());
        assertEquals("12345678901", domain.getCpf());
        assertEquals("1234567890", domain.getSusNumber());
        assertEquals("maria@email.com", domain.getEmail());
        assertEquals("11999998888", domain.getPhone());
        assertEquals(LocalDate.of(1990, 5, 15), domain.getBirthDate());
    }

    @Test
    @DisplayName("toResponse(Patient) mapeia campos corretamente")
    void toResponse_patient_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        Patient domain = Patient.builder()
                .id(id).name("Maria Silva").cpf("12345678901")
                .phone("11999998888").birthDate(LocalDate.of(1990, 5, 15))
                .active(true).build();

        PatientResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals("Maria Silva", dto.name());
        assertEquals("12345678901", dto.cpf());
        assertEquals("11999998888", dto.phone());
        assertEquals(LocalDate.of(1990, 5, 15), dto.birthDate());
        assertTrue(dto.active());
    }

    // ── Professional ──────────────────────────────────────

    @Test
    @DisplayName("toDomain(ProfessionalRequestDTO) mapeia campos corretamente")
    void toDomain_professionalRequest_mappsCorrectly() {
        ProfessionalRequestDTO dto = new ProfessionalRequestDTO(
                "Dr. João", "joao@email.com", "11988887777",
                "123456", ProfessionalRole.PROFESSIONAL, UUID.randomUUID());

        Professional domain = mapper.toDomain(dto);

        assertEquals("Dr. João", domain.getName());
        assertEquals("123456", domain.getRegistrationNumber());
        assertEquals(ProfessionalRole.PROFESSIONAL, domain.getRole());
        assertEquals("joao@email.com", domain.getEmail());
        assertEquals("11988887777", domain.getPhone());
    }

    @Test
    @DisplayName("toResponse(Professional) mapeia campos corretamente")
    void toResponse_professional_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        Professional domain = Professional.builder()
                .id(id).name("Dr. João").registrationNumber("123456")
                .role(ProfessionalRole.PROFESSIONAL).email("joao@email.com")
                .phone("11988887777").active(true).build();

        ProfessionalResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals("Dr. João", dto.name());
        assertEquals("123456", dto.registrationNumber());
        assertEquals(ProfessionalRole.PROFESSIONAL, dto.role());
        assertTrue(dto.active());
    }

    // ── Service ───────────────────────────────────────────

    @Test
    @DisplayName("toDomain(ServiceRequestDTO) mapeia campos corretamente")
    void toDomain_serviceRequest_mappsCorrectly() {
        ServiceRequestDTO dto = new ServiceRequestDTO("Consulta", "Descrição", 30, new BigDecimal("150.00"));

        br.com.belloinfo.saap_mvp.domain.model.Service domain = mapper.toDomain(dto);

        assertEquals("Consulta", domain.getName());
        assertEquals("Descrição", domain.getDescription());
        assertEquals(30, domain.getDurationMinutes());
        assertEquals(0, new BigDecimal("150.00").compareTo(domain.getPrice()));
    }

    @Test
    @DisplayName("toResponse(Service) mapeia campos corretamente")
    void toResponse_service_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        br.com.belloinfo.saap_mvp.domain.model.Service domain = br.com.belloinfo.saap_mvp.domain.model.Service.builder()
                .id(id).name("Consulta").description("Descrição")
                .durationMinutes(30).price(new BigDecimal("150.00"))
                .active(true).build();

        ServiceResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals("Consulta", dto.name());
        assertEquals("Descrição", dto.description());
        assertEquals(30, dto.durationMinutes());
        assertTrue(dto.active());
    }

    // ── Appointment ───────────────────────────────────────

    @Test
    @DisplayName("toResponse(Appointment) mapeia campos corretamente")
    void toResponse_appointment_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Maria").active(true).build();
        Professional professional = Professional.builder().id(UUID.randomUUID()).name("Dr. João").active(true).build();
        br.com.belloinfo.saap_mvp.domain.model.Service service = br.com.belloinfo.saap_mvp.domain.model.Service.builder().id(UUID.randomUUID()).name("Consulta").active(true).build();
        Appointment domain = Appointment.builder()
                .id(id)
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(now)
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P3)
                .priorityScore(3000L)
                .build();

        AppointmentResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals(AppointmentStatus.CONFIRMED, dto.status());
        assertEquals(PaymentMethod.PIX, dto.paymentMethod());
        assertEquals(PriorityLevel.P3, dto.priorityLevel());
        assertNotNull(dto.patient());
        assertEquals("Maria", dto.patient().name());
        assertNotNull(dto.professional());
        assertEquals("Dr. João", dto.professional().name());
        assertNotNull(dto.service());
        assertEquals("Consulta", dto.service().name());
    }

    // ── MedicalRecord ─────────────────────────────────────

    @Test
    @DisplayName("toResponse(MedicalRecord) mapeia campos corretamente")
    void toResponse_medicalRecord_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        MedicalRecordEntry entry = MedicalRecordEntry.builder()
                .id(UUID.randomUUID())
                .appointmentId(UUID.randomUUID())
                .professionalId(UUID.randomUUID())
                .evolution("Evolução teste")
                .createdAt(LocalDateTime.now())
                .build();
        MedicalRecord domain = MedicalRecord.builder()
                .id(id).patientId(patientId)
                .createdAt(LocalDateTime.now())
                .entries(java.util.List.of(entry))
                .build();

        MedicalRecordResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals(patientId, dto.patientId());
        assertNotNull(dto.entries());
        assertEquals(1, dto.entries().size());
        assertEquals("Evolução teste", dto.entries().getFirst().evolution());
    }

    // ── AuditLog ──────────────────────────────────────────

    @Test
    @DisplayName("toResponse(AuditLogWithEmail) mapeia campos customizados com @Mapping")
    void toResponse_auditLogWithEmail_mappsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recursoId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 4, 10, 30);
        AuditLog log = AuditLog.builder()
                .id(id)
                .timestamp(timestamp)
                .userId(userId)
                .action("CREATE")
                .recursoId(recursoId)
                .recursoTipo("Appointment")
                .ipAddress("192.168.1.1")
                .build();
        AuditLogWithEmail domain = new AuditLogWithEmail(log, "admin@clinica.com");

        AuditLogResponseDTO dto = mapper.toResponse(domain);

        assertEquals(id, dto.id());
        assertEquals(timestamp, dto.timestamp());
        assertEquals(userId, dto.userId());
        assertEquals("admin@clinica.com", dto.userEmail());
        assertEquals("CREATE", dto.action());
        assertEquals(recursoId, dto.recursoId());
        assertEquals("Appointment", dto.recursoTipo());
        assertEquals("192.168.1.1", dto.ipAddress());
    }
}

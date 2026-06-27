package br.com.belloinfo.saap_mvp.domain.model;

import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntitiesTest {

    @Test
    void shouldCreateAndManipulateUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@clinic.com")
                .password("encoded_pass")
                .role(UserRole.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertEquals(userId, user.getId());
        assertEquals("test@clinic.com", user.getEmail());
        assertEquals("encoded_pass", user.getPassword());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isActive());

        user.deactivate();
        assertFalse(user.isActive());

        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    void shouldCreateAndManipulatePatient() {
        UUID patientId = UUID.randomUUID();
        LocalDate birth = LocalDate.of(1990, 5, 10);
        Patient patient = Patient.builder()
                .id(patientId)
                .name("João Silva")
                .cpf("12345678901")
                .susNumber("123456789012345")
                .email("joao@gmail.com")
                .phone("11999999999")
                .birthDate(birth)
                .active(true)
                .build();

        assertEquals(patientId, patient.getId());
        assertEquals("João Silva", patient.getName());
        assertEquals("12345678901", patient.getCpf());
        assertEquals("123456789012345", patient.getSusNumber());
        assertEquals("joao@gmail.com", patient.getEmail());
        assertEquals("11999999999", patient.getPhone());
        assertEquals(birth, patient.getBirthDate());
        assertTrue(patient.isActive());

        patient.deactivate();
        assertFalse(patient.isActive());
    }

    @Test
    void shouldCreateAndManipulateProfessional() {
        UUID profId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Professional professional = Professional.builder()
                .id(profId)
                .name("Dr. André")
                .email("andre@clinic.com")
                .phone("11888888888")
                .registrationNumber("CRM-12345")
                .role(ProfessionalRole.PROFESSIONAL)
                .active(true)
                .userId(userId)
                .build();

        assertEquals(profId, professional.getId());
        assertEquals("Dr. André", professional.getName());
        assertEquals("andre@clinic.com", professional.getEmail());
        assertEquals("CRM-12345", professional.getRegistrationNumber());
        assertEquals(ProfessionalRole.PROFESSIONAL, professional.getRole());
        assertEquals(userId, professional.getUserId());
        assertTrue(professional.isActive());

        professional.deactivate();
        assertFalse(professional.isActive());
    }

    @Test
    void shouldCreateAndManipulateService() {
        UUID svcId = UUID.randomUUID();
        Service service = Service.builder()
                .id(svcId)
                .name("Consulta Geral")
                .description("Consulta de rotina")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();

        assertEquals(svcId, service.getId());
        assertEquals("Consulta Geral", service.getName());
        assertEquals("Consulta de rotina", service.getDescription());
        assertEquals(30, service.getDurationMinutes());
        assertEquals(BigDecimal.valueOf(150.00), service.getPrice());
        assertTrue(service.isActive());

        service.deactivate();
        assertFalse(service.isActive());
    }
}

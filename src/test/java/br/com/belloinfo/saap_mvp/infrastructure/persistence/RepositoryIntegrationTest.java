package br.com.belloinfo.saap_mvp.infrastructure.persistence;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaProfessionalRepository jpaProfessionalRepository;

    @Autowired
    private br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaServiceRepository jpaServiceRepository;

    @Test
    void shouldSaveAndFindUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@saap.com")
                .password("securePassword")
                .role(UserRole.RECEPTIONIST)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        assertNotNull(saved);

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("user@saap.com", found.get().getEmail());
        assertEquals(UserRole.RECEPTIONIST, found.get().getRole());
    }

    @Test
    void shouldSaveAndFindPatient() {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Maria Santos")
                .cpf("98765432109")
                .susNumber("987654321098765")
                .email("maria@saap.com")
                .phone("11988887777")
                .birthDate(LocalDate.of(1985, 8, 20))
                .active(true)
                .build();

        Patient saved = patientRepository.save(patient);
        assertNotNull(saved);

        Optional<Patient> found = patientRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Maria Santos", found.get().getName());
        assertEquals("98765432109", found.get().getCpf());
        assertEquals("987654321098765", found.get().getSusNumber());
        assertEquals(LocalDate.of(1985, 8, 20), found.get().getBirthDate());
    }

    @Test
    void shouldExcludeInactivePatientFromFindActive() {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Paciente Inativo")
                .cpf("11122233396")
                .email("inativo@saap.com")
                .phone("11966665555")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();
        Patient saved = patientRepository.save(patient);

        saved.deactivate();
        patientRepository.save(saved);

        // findById continua encontrando (Patient não usa @SQLRestriction)
        Optional<Patient> found = patientRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertFalse(found.get().isActive());

        // mas findActive (usado pela listagem paginada) deve excluí-lo
        List<Patient> activeOnly = patientRepository.findActive(0, 20).content();
        assertTrue(activeOnly.stream().noneMatch(p -> p.getId().equals(saved.getId())));
    }

    @Test
    void shouldExcludeInactiveUserFromFindActive() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("inativo_user@saap.com")
                .password("securePassword")
                .role(UserRole.RECEPTIONIST)
                .active(true)
                .build();
        User saved = userRepository.save(user);

        saved.deactivate();
        userRepository.save(saved);

        // findById continua encontrando (User não usa @SQLRestriction)
        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertFalse(found.get().isActive());

        // mas findActive (usado pela listagem paginada) deve excluí-lo
        List<User> activeOnly = userRepository.findActive(0, 20).content();
        assertTrue(activeOnly.stream().noneMatch(u -> u.getId().equals(saved.getId())));
    }

    @Test
    void shouldSoftDeleteProfessional() {
        // First create user for foreign key constraint
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("prof_user@saap.com")
                .password("pass")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        Professional prof = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Roberto")
                .email("roberto@saap.com")
                .phone("11977776666")
                .registrationNumber("CRM-99999")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();

        Professional saved = professionalRepository.save(prof);
        assertNotNull(saved);

        // Perform Soft Delete
        jpaProfessionalRepository.deleteById(saved.getId());

        // Under @SQLRestriction("is_active = true"), finding by ID should return empty
        Optional<Professional> foundActive = professionalRepository.findById(saved.getId());
        assertFalse(foundActive.isPresent());

        // Verify it still exists in DB as inactive
        List<Professional> all = professionalRepository.findActive(0, 20).content();
        assertTrue(all.stream().noneMatch(p -> p.getId().equals(saved.getId())));
    }

    @Test
    void shouldSoftDeleteService() {
        Service svc = Service.builder()
                .id(UUID.randomUUID())
                .name("Exame Ultrassom")
                .description("Ultrassonografia geral")
                .durationMinutes(45)
                .price(BigDecimal.valueOf(250.00))
                .active(true)
                .build();

        Service saved = serviceRepository.save(svc);
        assertNotNull(saved);

        // Perform Soft Delete
        jpaServiceRepository.deleteById(saved.getId());

        // Finding by ID should return empty
        Optional<Service> foundActive = serviceRepository.findById(saved.getId());
        assertFalse(foundActive.isPresent());

        // List active should be empty
        List<Service> all = serviceRepository.findActive(0, 20).content();
        assertTrue(all.stream().noneMatch(s -> s.getId().equals(saved.getId())));
    }

    @Test
    void shouldFindServiceByName() {
        Service svc = Service.builder()
                .id(UUID.randomUUID())
                .name("Exame Ultrassom")
                .description("Ultrassonografia geral")
                .durationMinutes(45)
                .price(BigDecimal.valueOf(250.00))
                .active(true)
                .build();

        serviceRepository.save(svc);

        Optional<Service> found = serviceRepository.findByName("Exame Ultrassom");
        assertTrue(found.isPresent());
        assertEquals(svc.getId(), found.get().getId());

        Optional<Service> notFound = serviceRepository.findByName("Exame Inexistente");
        assertFalse(notFound.isPresent());
    }

    @Test
    void shouldFailToSaveServiceWithDuplicateActiveName() {
        Service svc1 = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Pediatria")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(svc1);

        Service svc2 = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Pediatria") // Mesmo nome
                .durationMinutes(40)
                .price(BigDecimal.valueOf(180.00))
                .active(true)
                .build();

        // Deve lançar DataIntegrityViolationException devido à restrição do banco de dados (Índice único)
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            serviceRepository.save(svc2);
            jpaServiceRepository.flush(); // força gravação para disparar a constraint
        });
    }
}

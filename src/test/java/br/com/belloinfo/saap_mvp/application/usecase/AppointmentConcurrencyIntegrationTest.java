package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.repository.JpaAppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookAppointmentUseCase bookAppointmentUseCase;

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
        cleanDb();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor_concurrent@saap.com")
                .password("pwd")
                .role(UserRole.PROFESSIONAL)
                .active(true)
                .build();
        userRepository.save(user);

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Dr. Concurrent")
                .email("concurrent@saap.com")
                .phone("11988887777")
                .registrationNumber("CRM-77777")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(user.getId())
                .active(true)
                .build();
        professionalRepository.save(professional);

        patient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Concurrent Patient")
                .cpf("22233344455")
                .phone("11977776666")
                .birthDate(LocalDate.of(1995, 3, 15))
                .active(true)
                .build();
        patientRepository.save(patient);

        service = Service.builder()
                .id(UUID.randomUUID())
                .name("Consulta Geral")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(150.00))
                .active(true)
                .build();
        serviceRepository.save(service);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        jpaAppointmentRepository.deleteAll();
        professionalRepository.findAll().forEach(p -> {
            p.setUserId(null);
            professionalRepository.save(p);
        });
        jpaAppointmentRepository.flush();
        professionalRepository.findAll().forEach(p -> {
            jpaAppointmentRepository.deleteById(p.getId()); // In case professional soft delete restriction gets in the way, delete directly or make inactive
        });
        userRepository.findAll().forEach(u -> userRepository.save(u));
        // Hard delete all to avoid pollution
        try {
            // We use direct repository deletes since they are clean
            jpaAppointmentRepository.deleteAllInBatch();
            professionalRepository.findAll().forEach(p -> {
                // If soft delete is active, we might need direct sql delete or we just clear references
            });
        } catch (Exception ignored) {}
    }

    @Test
    void shouldHandleConcurrentBookingsSafely() throws InterruptedException, ExecutionException {
        LocalDateTime time = LocalDateTime.now().plusDays(10).withNano(0);
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                barrier.await(); // Garante que as duas threads iniciam a execução do UseCase juntas
                try {
                    bookAppointmentUseCase.execute(
                            patient.getId(),
                            professional.getId(),
                            service.getId(),
                            time,
                            PaymentMethod.PIX,
                            PriorityLevel.P5
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();

        // Exatamente um agendamento deve ter sucesso, e o outro deve falhar
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());

        // Verifica que apenas 1 agendamento foi persistido no banco
        long dbCount = jpaAppointmentRepository.count();
        assertEquals(1, dbCount);
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import br.com.belloinfo.saap_mvp.domain.exception.ScheduleConflictException;
import br.com.belloinfo.saap_mvp.infrastructure.web.exception.GlobalExceptionHandler;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookAppointmentUseCase bookAppointmentUseCase;
    @Mock
    private ConfirmAppointmentUseCase confirmAppointmentUseCase;
    @Mock
    private CancelAppointmentUseCase cancelAppointmentUseCase;
    @Mock
    private CheckInAppointmentUseCase checkInAppointmentUseCase;
    @Mock
    private StartAppointmentUseCase startAppointmentUseCase;
    @Mock
    private CompleteAppointmentUseCase completeAppointmentUseCase;
    @Mock
    private ListAppointmentsUseCase listAppointmentsUseCase;
    @Mock
    private FindAppointmentByIdUseCase findAppointmentByIdUseCase;
    @Mock
    private ConfirmAppointmentByTokenUseCase confirmAppointmentByTokenUseCase;
    @Mock
    private CancelAppointmentByTokenUseCase cancelAppointmentByTokenUseCase;
    @Mock
    private AcceptWaitlistOfferUseCase acceptWaitlistOfferUseCase;
    @Mock
    private DeclineWaitlistOfferUseCase declineWaitlistOfferUseCase;
    @Mock
    private CallNextPatientUseCase callNextPatientUseCase;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfessionalRepository professionalRepository;
    @Mock
    private br.com.belloinfo.saap_mvp.application.service.AuditService auditService;

    private final WebMapper mapper = org.mapstruct.factory.Mappers.getMapper(WebMapper.class);

    @BeforeEach
    void setUp() {
        AppointmentController controller = new AppointmentController(
                bookAppointmentUseCase,
                confirmAppointmentUseCase,
                cancelAppointmentUseCase,
                checkInAppointmentUseCase,
                startAppointmentUseCase,
                completeAppointmentUseCase,
                listAppointmentsUseCase,
                findAppointmentByIdUseCase,
                confirmAppointmentByTokenUseCase,
                cancelAppointmentByTokenUseCase,
                acceptWaitlistOfferUseCase,
                declineWaitlistOfferUseCase,
                callNextPatientUseCase,
                userRepository,
                professionalRepository,
                auditService,
                mapper
        );

        org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping = 
                new org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping();
        handlerMapping.setPathPrefixes(java.util.Map.of(
                "/api/v1", c -> c.equals(AppointmentController.class)
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomHandlerMapping(() -> handlerMapping)
                .build();
    }

    @Test
    void shouldBookAppointment() throws Exception {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now().plusDays(2);

        Appointment appointment = Appointment.builder()
                .id(id)
                .patient(Patient.builder().id(patientId).name("Patient").build())
                .professional(Professional.builder().id(professionalId).name("Doc").build())
                .service(Service.builder().id(serviceId).name("Svc").build())
                .dateTime(time)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();

        when(bookAppointmentUseCase.execute(eq(patientId), eq(professionalId), eq(serviceId), any(LocalDateTime.class), eq(PaymentMethod.PIX), any()))
                .thenReturn(appointment);

        String requestJson = String.format("""
                {
                  "patientId": "%s",
                  "professionalId": "%s",
                  "serviceId": "%s",
                  "dateTime": "%s",
                  "paymentMethod": "PIX",
                  "declaredPriority": "P5"
                }
                """, patientId, professionalId, serviceId, time);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.paymentMethod", is("PIX")));
    }

    @Test
    void shouldReturnConflictOnDoubleBooking() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now().plusDays(2);

        when(bookAppointmentUseCase.execute(eq(patientId), eq(professionalId), eq(serviceId), any(LocalDateTime.class), eq(PaymentMethod.PIX), any()))
                .thenThrow(new ScheduleConflictException("O horário solicitado não está disponível. Tente outro horário."));

        String requestJson = String.format("""
                {
                  "patientId": "%s",
                  "professionalId": "%s",
                  "serviceId": "%s",
                  "dateTime": "%s",
                  "paymentMethod": "PIX",
                  "declaredPriority": "P5"
                }
                """, patientId, professionalId, serviceId, time);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("horário solicitado não está disponível")));
    }

    @Test
    void shouldConfirmAppointment() throws Exception {
        UUID id = UUID.randomUUID();
        Appointment appointment = Appointment.builder()
                .id(id)
                .status(AppointmentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();

        when(confirmAppointmentUseCase.execute(id)).thenReturn(appointment);

        mockMvc.perform(put("/api/v1/appointments/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void shouldCheckInAppointment() throws Exception {
        UUID id = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();
        Appointment appointment = Appointment.builder()
                .id(id)
                .status(AppointmentStatus.ARRIVED)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P1)
                .priorityVerifiedBy(receptionistId)
                .priorityNotes("TEA")
                .priorityScore(1000L)
                .build();

        when(checkInAppointmentUseCase.execute(eq(id), eq(PriorityLevel.P1), eq(receptionistId), eq("TEA"), anyString()))
                .thenReturn(appointment);

        String requestJson = String.format("""
                {
                  "verifiedLevel": "P1",
                  "verifiedBy": "%s",
                  "notes": "TEA"
                }
                """, receptionistId);

        mockMvc.perform(put("/api/v1/appointments/" + id + "/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARRIVED")))
                .andExpect(jsonPath("$.priorityLevel", is("P1")))
                .andExpect(jsonPath("$.priorityNotes", is("TEA")));
    }

    @Test
    void shouldListAppointments() throws Exception {
        UUID id = UUID.randomUUID();
        Appointment appointment = Appointment.builder()
                .id(id)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();

        when(listAppointmentsUseCase.execute(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new br.com.belloinfo.saap_mvp.domain.model.PageResult<>(Collections.singletonList(appointment), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(id.toString())));
    }

    @Test
    void shouldCallNextPatient() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        String email = "doc@example.com";

        User user = User.builder().id(userId).email(email).build();
        Professional professional = Professional.builder().id(professionalId).build();
        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .status(AppointmentStatus.CALLING)
                .build();

        // Mock SecurityContext
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(professionalRepository.findByUserId(userId)).thenReturn(Optional.of(professional));
        when(callNextPatientUseCase.execute(eq(professionalId), eq(userId), anyString())).thenReturn(appointment);

        mockMvc.perform(post("/api/v1/appointments/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appointmentId.toString())))
                .andExpect(jsonPath("$.status", is("CALLING")));
    }
}

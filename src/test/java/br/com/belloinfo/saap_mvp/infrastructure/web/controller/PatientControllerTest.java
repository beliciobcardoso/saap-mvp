package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.infrastructure.web.exception.GlobalExceptionHandler;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreatePatientUseCase createPatientUseCase;
    @Mock
    private FindPatientByIdUseCase findPatientByIdUseCase;
    @Mock
    private ListActivePatientsUseCase listActivePatientsUseCase;
    @Mock
    private UpdatePatientUseCase updatePatientUseCase;
    @Mock
    private DeactivatePatientUseCase deactivatePatientUseCase;

    private final WebMapper mapper = org.mapstruct.factory.Mappers.getMapper(WebMapper.class);

    @BeforeEach
    void setUp() {
        PatientController controller = new PatientController(
                createPatientUseCase,
                findPatientByIdUseCase,
                listActivePatientsUseCase,
                updatePatientUseCase,
                deactivatePatientUseCase,
                mapper
        );

        org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping = 
                new org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping();
        handlerMapping.setPathPrefixes(java.util.Map.of(
                "/api/v1", c -> c.equals(PatientController.class)
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomHandlerMapping(() -> handlerMapping)
                .build();
    }

    @Test
    void shouldCreatePatient() throws Exception {
        UUID id = UUID.randomUUID();
        Patient savedPatient = Patient.builder()
                .id(id)
                .name("John Doe")
                .cpf("52998224725")
                .susNumber("123456789012345")
                .email("john.doe@example.com")
                .phone("11988887777")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();

        when(createPatientUseCase.execute(any(Patient.class))).thenReturn(savedPatient);

        String requestJson = """
                {
                  "name": "John Doe",
                  "cpf": "52998224725",
                  "susNumber": "123456789012345",
                  "email": "john.doe@example.com",
                  "phone": "11988887777",
                  "birthDate": "1990-01-01"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.cpf", is("52998224725")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturnConflictWhenCpfExists() throws Exception {
        when(createPatientUseCase.execute(any(Patient.class)))
                .thenThrow(new IllegalArgumentException("CPF já cadastrado"));

        String requestJson = """
                {
                  "name": "Jane Doe",
                  "cpf": "52998224725",
                  "susNumber": "987654321098765",
                  "email": "jane.doe@example.com",
                  "phone": "11988886666",
                  "birthDate": "1995-05-05"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("CPF já cadastrado")));
    }

    @Test
    void shouldFindPatientById() throws Exception {
        UUID id = UUID.randomUUID();
        Patient patient = Patient.builder()
                .id(id)
                .name("John Doe")
                .cpf("52998224725")
                .active(true)
                .build();

        when(findPatientByIdUseCase.execute(id)).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/v1/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("John Doe")));
    }

    @Test
    void shouldReturnNotFoundWhenPatientDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(findPatientByIdUseCase.execute(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/patients/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListActivePatients() throws Exception {
        UUID id = UUID.randomUUID();
        Patient patient = Patient.builder().id(id).name("John Doe").active(true).build();
        when(listActivePatientsUseCase.execute()).thenReturn(Collections.singletonList(patient));

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("John Doe")));
    }

    @Test
    void shouldUpdatePatient() throws Exception {
        UUID id = UUID.randomUUID();
        Patient updatedPatient = Patient.builder()
                .id(id)
                .name("John Doe Updated")
                .cpf("52998224725")
                .active(true)
                .build();

        when(updatePatientUseCase.execute(eq(id), any(Patient.class))).thenReturn(updatedPatient);

        String requestJson = """
                {
                  "name": "John Doe Updated",
                  "cpf": "52998224725",
                  "susNumber": "123456789012345",
                  "email": "john.updated@example.com",
                  "phone": "11988887778",
                  "birthDate": "1990-01-01"
                }
                """;

        mockMvc.perform(put("/api/v1/patients/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("John Doe Updated")));
    }

    @Test
    void shouldDeactivatePatient() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/patients/" + id))
                .andExpect(status().isNoContent());

        verify(deactivatePatientUseCase, times(1)).execute(id);
    }
}

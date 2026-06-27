package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
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

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateProfessionalUseCase createProfessionalUseCase;
    @Mock
    private FindProfessionalByIdUseCase findProfessionalByIdUseCase;
    @Mock
    private ListActiveProfessionalsUseCase listActiveProfessionalsUseCase;
    @Mock
    private UpdateProfessionalUseCase updateProfessionalUseCase;
    @Mock
    private DeactivateProfessionalUseCase deactivateProfessionalUseCase;

    private final WebMapper mapper = org.mapstruct.factory.Mappers.getMapper(WebMapper.class);

    @BeforeEach
    void setUp() {
        ProfessionalController controller = new ProfessionalController(
                createProfessionalUseCase,
                findProfessionalByIdUseCase,
                listActiveProfessionalsUseCase,
                updateProfessionalUseCase,
                deactivateProfessionalUseCase,
                mapper
        );

        org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping = 
                new org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping();
        handlerMapping.setPathPrefixes(java.util.Map.of(
                "/api/v1", c -> c.equals(ProfessionalController.class)
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomHandlerMapping(() -> handlerMapping)
                .build();
    }

    @Test
    void shouldCreateProfessional() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Professional savedProfessional = Professional.builder()
                .id(id)
                .name("Dr. House")
                .email("house@example.com")
                .phone("11999998888")
                .registrationNumber("CRM12345")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(userId)
                .active(true)
                .build();

        when(createProfessionalUseCase.execute(any(Professional.class))).thenReturn(savedProfessional);

        String requestJson = String.format("""
                {
                  "name": "Dr. House",
                  "email": "house@example.com",
                  "phone": "11999998888",
                  "registrationNumber": "CRM12345",
                  "role": "PROFESSIONAL",
                  "userId": "%s"
                }
                """, userId);

        mockMvc.perform(post("/api/v1/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("Dr. House")))
                .andExpect(jsonPath("$.registrationNumber", is("CRM12345")))
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturnConflictWhenRegistrationNumberExists() throws Exception {
        when(createProfessionalUseCase.execute(any(Professional.class)))
                .thenThrow(new IllegalArgumentException("Registro profissional já cadastrado"));

        String requestJson = """
                {
                  "name": "Dr. Wilson",
                  "email": "wilson@example.com",
                  "phone": "11999997777",
                  "registrationNumber": "CRM12345",
                  "role": "PROFESSIONAL",
                  "userId": null
                }
                """;

        mockMvc.perform(post("/api/v1/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Registro profissional já cadastrado")));
    }

    @Test
    void shouldFindProfessionalById() throws Exception {
        UUID id = UUID.randomUUID();
        Professional professional = Professional.builder()
                .id(id)
                .name("Dr. House")
                .registrationNumber("CRM12345")
                .active(true)
                .build();

        when(findProfessionalByIdUseCase.execute(id)).thenReturn(Optional.of(professional));

        mockMvc.perform(get("/api/v1/professionals/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("Dr. House")));
    }

    @Test
    void shouldReturnNotFoundWhenProfessionalDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(findProfessionalByIdUseCase.execute(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/professionals/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListActiveProfessionals() throws Exception {
        UUID id = UUID.randomUUID();
        Professional professional = Professional.builder().id(id).name("Dr. House").active(true).build();
        when(listActiveProfessionalsUseCase.execute()).thenReturn(Collections.singletonList(professional));

        mockMvc.perform(get("/api/v1/professionals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Dr. House")));
    }

    @Test
    void shouldUpdateProfessional() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Professional updatedProfessional = Professional.builder()
                .id(id)
                .name("Dr. House Updated")
                .email("house.up@example.com")
                .phone("11999998889")
                .registrationNumber("CRM12345")
                .role(ProfessionalRole.PROFESSIONAL)
                .userId(userId)
                .active(true)
                .build();

        when(updateProfessionalUseCase.execute(eq(id), any(Professional.class))).thenReturn(updatedProfessional);

        String requestJson = String.format("""
                {
                  "name": "Dr. House Updated",
                  "email": "house.up@example.com",
                  "phone": "11999998889",
                  "registrationNumber": "CRM12345",
                  "role": "PROFESSIONAL",
                  "userId": "%s"
                }
                """, userId);

        mockMvc.perform(put("/api/v1/professionals/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Dr. House Updated")));
    }

    @Test
    void shouldDeactivateProfessional() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/professionals/" + id))
                .andExpect(status().isNoContent());

        verify(deactivateProfessionalUseCase, times(1)).execute(id);
    }
}

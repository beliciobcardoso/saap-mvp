package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Service;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ServiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateServiceUseCase createServiceUseCase;
    @Mock
    private FindServiceByIdUseCase findServiceByIdUseCase;
    @Mock
    private ListActiveServicesUseCase listActiveServicesUseCase;
    @Mock
    private UpdateServiceUseCase updateServiceUseCase;
    @Mock
    private DeactivateServiceUseCase deactivateServiceUseCase;

    private final WebMapper mapper = org.mapstruct.factory.Mappers.getMapper(WebMapper.class);

    @BeforeEach
    void setUp() {
        ServiceController controller = new ServiceController(
                createServiceUseCase,
                findServiceByIdUseCase,
                listActiveServicesUseCase,
                updateServiceUseCase,
                deactivateServiceUseCase,
                mapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateService() throws Exception {
        UUID id = UUID.randomUUID();
        Service savedService = Service.builder()
                .id(id)
                .name("Consulta Geral")
                .description("Consulta de rotina")
                .durationMinutes(30)
                .price(new BigDecimal("150.00"))
                .active(true)
                .build();

        when(createServiceUseCase.execute(any(Service.class))).thenReturn(savedService);

        String requestJson = """
                {
                  "name": "Consulta Geral",
                  "description": "Consulta de rotina",
                  "durationMinutes": 30,
                  "price": 150.00
                }
                """;

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("Consulta Geral")))
                .andExpect(jsonPath("$.durationMinutes", is(30)))
                .andExpect(jsonPath("$.price", is(150.0)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldFindServiceById() throws Exception {
        UUID id = UUID.randomUUID();
        Service service = Service.builder()
                .id(id)
                .name("Consulta Geral")
                .active(true)
                .build();

        when(findServiceByIdUseCase.execute(id)).thenReturn(Optional.of(service));

        mockMvc.perform(get("/api/services/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("Consulta Geral")));
    }

    @Test
    void shouldReturnNotFoundWhenServiceDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(findServiceByIdUseCase.execute(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/services/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListActiveServices() throws Exception {
        UUID id = UUID.randomUUID();
        Service service = Service.builder().id(id).name("Consulta Geral").active(true).build();
        when(listActiveServicesUseCase.execute()).thenReturn(Collections.singletonList(service));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Consulta Geral")));
    }

    @Test
    void shouldUpdateService() throws Exception {
        UUID id = UUID.randomUUID();
        Service updatedService = Service.builder()
                .id(id)
                .name("Consulta Geral Premium")
                .description("Consulta de rotina premium")
                .durationMinutes(45)
                .price(new BigDecimal("250.00"))
                .active(true)
                .build();

        when(updateServiceUseCase.execute(eq(id), any(Service.class))).thenReturn(updatedService);

        String requestJson = """
                {
                  "name": "Consulta Geral Premium",
                  "description": "Consulta de rotina premium",
                  "durationMinutes": 45,
                  "price": 250.00
                }
                """;

        mockMvc.perform(put("/api/services/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Consulta Geral Premium")));
    }

    @Test
    void shouldDeactivateService() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/services/" + id))
                .andExpect(status().isNoContent());

        verify(deactivateServiceUseCase, times(1)).execute(id);
    }
}

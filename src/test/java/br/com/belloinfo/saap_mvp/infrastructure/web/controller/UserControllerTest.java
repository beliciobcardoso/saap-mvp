package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
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
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateUserUseCase createUserUseCase;
    @Mock
    private FindUserByIdUseCase findUserByIdUseCase;
    @Mock
    private ListActiveUsersUseCase listActiveUsersUseCase;
    @Mock
    private UpdateUserUseCase updateUserUseCase;
    @Mock
    private DeactivateUserUseCase deactivateUserUseCase;

    private final WebMapper mapper = org.mapstruct.factory.Mappers.getMapper(WebMapper.class);

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(
                createUserUseCase,
                findUserByIdUseCase,
                listActiveUsersUseCase,
                updateUserUseCase,
                deactivateUserUseCase,
                mapper
        );
        
        org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping = 
                new org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping();
        handlerMapping.setPathPrefixes(java.util.Map.of(
                "/api/v1", c -> c.equals(UserController.class)
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomHandlerMapping(() -> handlerMapping)
                .build();
    }

    @Test
    void shouldCreateUser() throws Exception {
        UUID id = UUID.randomUUID();
        User savedUser = User.builder()
                .id(id)
                .email("user.test@example.com")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        when(createUserUseCase.execute(any(User.class))).thenReturn(savedUser);

        String requestJson = """
                {
                  "email": "user.test@example.com",
                  "password": "secretpassword",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.email", is("user.test@example.com")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturnConflictWhenEmailExists() throws Exception {
        when(createUserUseCase.execute(any(User.class)))
                .thenThrow(new IllegalArgumentException("E-mail já cadastrado"));

        String requestJson = """
                {
                  "email": "user.test@example.com",
                  "password": "secretpassword",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("E-mail já cadastrado")));
    }

    @Test
    void shouldFindUserById() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("user@example.com").active(true).build();
        when(findUserByIdUseCase.execute(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("user@example.com")));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(findUserByIdUseCase.execute(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListActiveUsers() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("user@example.com").active(true).build();
        when(listActiveUsersUseCase.execute()).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("user@example.com")));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UUID id = UUID.randomUUID();
        User updatedUser = User.builder().id(id).email("user.updated@example.com").role(UserRole.PROFESSIONAL).active(true).build();
        when(updateUserUseCase.execute(eq(id), any(User.class))).thenReturn(updatedUser);

        String requestJson = """
                {
                  "email": "user.updated@example.com",
                  "password": "newpassword123",
                  "role": "PROFESSIONAL"
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("user.updated@example.com")))
                .andExpect(jsonPath("$.role", is("PROFESSIONAL")));
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/" + id))
                .andExpect(status().isNoContent());

        verify(deactivateUserUseCase, times(1)).execute(id);
    }
}

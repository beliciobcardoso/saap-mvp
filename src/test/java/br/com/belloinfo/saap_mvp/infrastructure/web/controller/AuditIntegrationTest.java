package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.BaseIntegrationTest;
import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.UserRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuditIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("audit_admin@saap.com")
                .password(passwordEncoder.encode("adminPass123"))
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        userRepository.save(adminUser);

        adminToken = generateTestToken("audit_admin@saap.com", UserRole.ADMIN);
    }

    @Test
    void shouldAuditUserCreationAndDeactivation() throws Exception {
        UserRequestDTO request = new UserRequestDTO("new_audited_user@saap.com", "pwd123", UserRole.RECEPTIONIST);

        // 1. Test creation
        String responseContent = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extrai o ID do usuário criado a partir do JSON da resposta
        String createdUserIdStr = objectMapper.readTree(responseContent).get("id").asText();
        UUID createdUserId = UUID.fromString(createdUserIdStr);

        // Verifica que o log de auditoria foi gravado corretamente
        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog creationLog = logs.stream()
                .filter(l -> "CADASTRO_USUARIO".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(creationLog).isNotNull();
        assertThat(creationLog.getRecursoId()).isEqualTo(createdUserId);
        assertThat(creationLog.getRecursoTipo()).isEqualTo("USER");
        assertThat(creationLog.getUserId()).isEqualTo(adminUser.getId());

        // 2. Test deactivation
        mockMvc.perform(delete("/api/v1/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        logs = auditLogRepository.findAll();
        AuditLog deactivationLog = logs.stream()
                .filter(l -> "DESATIVACAO_USUARIO".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(deactivationLog).isNotNull();
        assertThat(deactivationLog.getRecursoId()).isEqualTo(createdUserId);
        assertThat(deactivationLog.getRecursoTipo()).isEqualTo("USER");
        assertThat(deactivationLog.getUserId()).isEqualTo(adminUser.getId());
    }

    @Test
    void shouldAuditUserLogin() throws Exception {
        br.com.belloinfo.saap_mvp.infrastructure.web.dto.LoginRequestDTO loginRequest = 
                new br.com.belloinfo.saap_mvp.infrastructure.web.dto.LoginRequestDTO("audit_admin@saap.com", "adminPass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog loginLog = logs.stream()
                .filter(l -> "LOGIN_USUARIO".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(loginLog).isNotNull();
        assertThat(loginLog.getRecursoId()).isEqualTo(adminUser.getId());
        assertThat(loginLog.getRecursoTipo()).isEqualTo("USER");
        assertThat(loginLog.getUserId()).isEqualTo(adminUser.getId());
    }
}

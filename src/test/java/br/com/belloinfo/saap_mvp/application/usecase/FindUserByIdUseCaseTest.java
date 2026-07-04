package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindUserByIdUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private FindUserByIdUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new FindUserByIdUseCase(userRepository);
    }

    @Test
    @DisplayName("retorna usuário quando encontrado pelo id")
    void execute_existingUser_returnsUser() {
        User user = User.builder().id(userId).email("usuario@clinica.com").role(UserRole.PROFESSIONAL).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Optional<User> result = useCase.execute(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
    }

    @Test
    @DisplayName("retorna Optional vazio quando usuário não é encontrado")
    void execute_missingUser_returnsEmptyOptional() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }
}

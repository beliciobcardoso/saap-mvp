package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeactivateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private DeactivateUserUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeactivateUserUseCase(userRepository);
    }

    private User user(boolean active) {
        return User.builder().id(userId).email("test@email.com").role(UserRole.ADMIN).active(active).build();
    }

    @Test
    @DisplayName("desativa usuário ativo e persiste a alteração")
    void execute_activeUser_deactivatesAndSaves() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(true)));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        useCase.execute(userId);

        verify(userRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    @DisplayName("mantém idempotência ao desativar usuário já inativo")
    void execute_alreadyInactiveUser_staysInactiveAndSaves() {
        User inactiveUser = user(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

        useCase.execute(userId);

        assertFalse(inactiveUser.isActive());
        verify(userRepository).save(inactiveUser);
    }

    @Test
    @DisplayName("lança exceção quando usuário não é encontrado")
    void execute_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(userId));

        verify(userRepository, never()).save(any());
    }
}

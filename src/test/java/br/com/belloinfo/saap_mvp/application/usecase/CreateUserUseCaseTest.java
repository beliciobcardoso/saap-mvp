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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateUserUseCase(userRepository, passwordEncoder);
    }

    private User newUser() {
        return User.builder()
                .email("novo@clinica.com")
                .password("senhaOriginal")
                .role(UserRole.RECEPTIONIST)
                .build();
    }

    @Test
    @DisplayName("cria usuário com senha codificada e ativa a conta")
    void execute_withValidUser_createsActiveUserWithEncodedPassword() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senhaOriginal")).thenReturn("senhaCodificada");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = useCase.execute(user);

        assertEquals("senhaCodificada", created.getPassword());
        assertTrue(created.isActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("rejeita criação quando e-mail já está cadastrado")
    void execute_withExistingEmail_throwsIllegalArgumentException() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(newUser()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(user));

        assertEquals("E-mail já cadastrado", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}

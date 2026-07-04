package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException;
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
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UpdateUserUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdateUserUseCase(userRepository, passwordEncoder);
    }

    private User existingUser() {
        return User.builder()
                .id(userId)
                .email("atual@clinica.com")
                .password("senhaAntigaCodificada")
                .role(UserRole.ASSISTANT)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("rejeita atualização quando usuário não é encontrado")
    void execute_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        User updatedUser = User.builder().email("novo@clinica.com").role(UserRole.ADMIN).build();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(userId, updatedUser));

        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejeita atualização quando novo e-mail já está em uso por outro usuário")
    void execute_emailInUseByAnotherUser_throwsIllegalArgumentException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser()));
        User updatedUser = User.builder().email("outro@clinica.com").role(UserRole.ADMIN).build();
        when(userRepository.findByEmail("outro@clinica.com"))
                .thenReturn(Optional.of(User.builder().email("outro@clinica.com").build()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(userId, updatedUser));

        assertEquals("E-mail já em uso por outro usuário", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualiza e-mail quando o novo endereço está disponível")
    void execute_newEmailAvailable_updatesEmailSuccessfully() {
        User existing = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        User updatedUser = User.builder().email("disponivel@clinica.com").role(existing.getRole()).build();
        when(userRepository.findByEmail("disponivel@clinica.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(userId, updatedUser);

        assertEquals("disponivel@clinica.com", result.getEmail());
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("atualiza dados sem verificar duplicidade quando e-mail permanece o mesmo")
    void execute_sameEmail_skipsDuplicateCheckAndUpdates() {
        User existing = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        User updatedUser = User.builder().email(existing.getEmail()).role(UserRole.ADMIN).build();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(userId, updatedUser);

        assertEquals(UserRole.ADMIN, result.getRole());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("codifica e atualiza a senha quando uma nova senha é informada")
    void execute_withNewPassword_encodesAndUpdatesPassword() {
        User existing = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        User updatedUser = User.builder().email(existing.getEmail()).role(existing.getRole()).password("novaSenha").build();
        when(passwordEncoder.encode("novaSenha")).thenReturn("novaSenhaCodificada");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(userId, updatedUser);

        assertEquals("novaSenhaCodificada", result.getPassword());
    }

    @Test
    @DisplayName("mantém senha existente quando nova senha é nula")
    void execute_withNullPassword_keepsExistingPassword() {
        User existing = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        User updatedUser = User.builder().email(existing.getEmail()).role(existing.getRole()).password(null).build();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(userId, updatedUser);

        assertEquals("senhaAntigaCodificada", result.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("mantém senha existente quando nova senha é vazia")
    void execute_withBlankPassword_keepsExistingPassword() {
        User existing = existingUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        User updatedUser = User.builder().email(existing.getEmail()).role(existing.getRole()).password("").build();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(userId, updatedUser);

        assertEquals("senhaAntigaCodificada", result.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }
}

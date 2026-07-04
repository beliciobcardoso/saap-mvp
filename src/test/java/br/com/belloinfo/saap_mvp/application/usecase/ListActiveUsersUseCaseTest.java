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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListActiveUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private ListActiveUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActiveUsersUseCase(userRepository);
    }

    private User user(String email, UserRole role, boolean active) {
        return User.builder().id(UUID.randomUUID()).email(email).role(role).active(active).build();
    }

    @Test
    @DisplayName("retorna apenas usuários ativos, filtrando os inativos")
    void execute_mixedUsers_returnsOnlyActiveOnes() {
        User active1 = user("admin@email.com", UserRole.ADMIN, true);
        User inactive = user("inactive@email.com", UserRole.RECEPTIONIST, false);
        User active2 = user("pro@email.com", UserRole.PROFESSIONAL, true);
        when(userRepository.findAll()).thenReturn(List.of(active1, inactive, active2));

        List<User> result = useCase.execute();

        assertEquals(2, result.size());
        assertTrue(result.contains(active1));
        assertTrue(result.contains(active2));
        assertFalse(result.contains(inactive));
    }

    @Test
    @DisplayName("retorna lista vazia quando não há usuários cadastrados")
    void execute_noUsers_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = useCase.execute();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("retorna lista vazia quando todos os usuários estão inativos")
    void execute_allInactiveUsers_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of(
                user("a@email.com", UserRole.ADMIN, false),
                user("b@email.com", UserRole.PATIENT, false)
        ));

        List<User> result = useCase.execute();

        assertTrue(result.isEmpty());
    }
}

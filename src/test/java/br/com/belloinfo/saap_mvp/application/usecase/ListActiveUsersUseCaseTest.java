package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
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
    @DisplayName("delega paginação ao repositório e retorna o PageResult obtido")
    void execute_delegatesToRepositoryWithPageAndSize() {
        User active1 = user("admin@email.com", UserRole.ADMIN, true);
        User active2 = user("pro@email.com", UserRole.PROFESSIONAL, true);
        PageResult<User> expected = new PageResult<>(List.of(active1, active2), 0, 20, 2, 1);
        when(userRepository.findActive(0, 20)).thenReturn(expected);

        PageResult<User> result = useCase.execute(0, 20);

        assertEquals(expected, result);
        verify(userRepository).findActive(0, 20);
    }

    @Test
    @DisplayName("retorna PageResult vazio quando não há usuários cadastrados")
    void execute_noUsers_returnsEmptyPageResult() {
        PageResult<User> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(userRepository.findActive(0, 20)).thenReturn(expected);

        PageResult<User> result = useCase.execute(0, 20);

        assertTrue(result.content().isEmpty());
    }
}

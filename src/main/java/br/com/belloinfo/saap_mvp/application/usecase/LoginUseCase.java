package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;

    public User execute(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        if (!user.isActive()) {
            throw new AccessDeniedException("Usuário inativo");
        }

        return user;
    }
}

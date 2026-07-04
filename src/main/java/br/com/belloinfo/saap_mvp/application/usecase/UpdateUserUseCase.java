package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UpdateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UpdateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User execute(UUID id, User updatedUser) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException("Usuário não encontrado"));

        if (!existing.getEmail().equals(updatedUser.getEmail()) 
            && userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-mail já em uso por outro usuário");
        }

        existing.setEmail(updatedUser.getEmail());
        existing.setRole(updatedUser.getRole());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(existing);
    }
}

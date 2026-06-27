package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ListActiveUsersUseCase {
    private final UserRepository userRepository;

    public ListActiveUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> execute() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .collect(java.util.stream.Collectors.toList());
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ListActiveUsersUseCase {
    private final UserRepository userRepository;

    public ListActiveUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResult<User> execute(int page, int size) {
        return userRepository.findActive(page, size);
    }
}

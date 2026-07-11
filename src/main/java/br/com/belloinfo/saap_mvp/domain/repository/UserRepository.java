package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    List<User> findByIdIn(List<UUID> ids);
    PageResult<User> findActive(int page, int size);
}

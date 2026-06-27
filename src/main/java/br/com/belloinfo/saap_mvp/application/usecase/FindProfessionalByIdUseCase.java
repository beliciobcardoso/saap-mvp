package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class FindProfessionalByIdUseCase {
    private final ProfessionalRepository professionalRepository;

    public FindProfessionalByIdUseCase(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public Optional<Professional> execute(UUID id) {
        return professionalRepository.findById(id);
    }
}

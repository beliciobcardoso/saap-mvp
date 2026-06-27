package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ListActiveProfessionalsUseCase {
    private final ProfessionalRepository professionalRepository;

    public ListActiveProfessionalsUseCase(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public List<Professional> execute() {
        return professionalRepository.findAllActive();
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.springframework.stereotype.Service;

@Service
public class ListActiveProfessionalsUseCase {
    private final ProfessionalRepository professionalRepository;

    public ListActiveProfessionalsUseCase(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public PageResult<Professional> execute(int page, int size) {
        return professionalRepository.findActive(page, size);
    }
}

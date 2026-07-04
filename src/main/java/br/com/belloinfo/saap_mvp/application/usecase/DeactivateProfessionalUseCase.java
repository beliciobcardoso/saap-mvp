package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class DeactivateProfessionalUseCase {
    private final ProfessionalRepository professionalRepository;

    public DeactivateProfessionalUseCase(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public void execute(UUID id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException("Profissional não encontrado"));
        professional.deactivate();
        professionalRepository.save(professional);
    }
}

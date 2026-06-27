package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateProfessionalUseCase {
    private final ProfessionalRepository professionalRepository;

    public CreateProfessionalUseCase(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public Professional execute(Professional professional) {
        if (professional.getRegistrationNumber() != null 
            && professionalRepository.findByRegistrationNumber(professional.getRegistrationNumber()).isPresent()) {
            throw new IllegalArgumentException("Registro profissional já cadastrado");
        }
        professional.activate();
        return professionalRepository.save(professional);
    }
}

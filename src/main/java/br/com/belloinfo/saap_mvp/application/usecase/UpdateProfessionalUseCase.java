package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UpdateProfessionalUseCase {
    private final ProfessionalRepository professionalRepository;

    public UpdateProfessionalUseCase(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public Professional execute(UUID id, Professional updated) {
        Professional existing = professionalRepository.findById(id)
                .orElseThrow(() -> new br.com.belloinfo.saap_mvp.domain.exception.ResourceNotFoundException("Profissional não encontrado"));

        if (updated.getRegistrationNumber() != null 
            && !updated.getRegistrationNumber().equals(existing.getRegistrationNumber()) 
            && professionalRepository.findByRegistrationNumber(updated.getRegistrationNumber()).isPresent()) {
            throw new IllegalArgumentException("Registro profissional já cadastrado por outro profissional");
        }

        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setRegistrationNumber(updated.getRegistrationNumber());
        existing.setRole(updated.getRole());
        existing.setUserId(updated.getUserId());
        return professionalRepository.save(existing);
    }
}

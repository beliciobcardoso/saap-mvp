package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UpdatePatientUseCase {
    private final PatientRepository patientRepository;

    public UpdatePatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient execute(UUID id, Patient updated) {
        Patient existing = patientRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado"));

        if (updated.getCpf() != null && !updated.getCpf().equals(existing.getCpf()) 
            && patientRepository.findByCpf(updated.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado por outro paciente");
        }

        existing.setName(updated.getName());
        existing.setCpf(updated.getCpf());
        existing.setSusNumber(updated.getSusNumber());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setBirthDate(updated.getBirthDate());
        return patientRepository.save(existing);
    }
}

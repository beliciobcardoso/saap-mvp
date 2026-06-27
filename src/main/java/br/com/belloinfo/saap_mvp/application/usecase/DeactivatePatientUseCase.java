package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class DeactivatePatientUseCase {
    private final PatientRepository patientRepository;

    public DeactivatePatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public void execute(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Paciente não encontrado"));
        patient.deactivate();
        patientRepository.save(patient);
    }
}

package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class CreatePatientUseCase {
    private final PatientRepository patientRepository;

    public CreatePatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient execute(Patient patient) {
        if (patient.getCpf() != null && patientRepository.findByCpf(patient.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
        patient.activate();
        return patientRepository.save(patient);
    }
}

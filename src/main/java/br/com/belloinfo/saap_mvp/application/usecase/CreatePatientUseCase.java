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
        // Normaliza o CPF: remove qualquer formatação e mantém apenas os 11 dígitos.
        // Aceita tanto "697.028.342-95" quanto "69702834295" como entrada válida.
        if (patient.getCpf() != null) {
            patient.setCpf(patient.getCpf().replaceAll("\\D", ""));
        }

        if (patient.getCpf() != null && patientRepository.findByCpf(patient.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
        patient.activate();
        return patientRepository.save(patient);
    }
}

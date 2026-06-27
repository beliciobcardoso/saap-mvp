package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListActivePatientsUseCase {
    private final PatientRepository patientRepository;

    public ListActivePatientsUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> execute() {
        return patientRepository.findAll().stream()
                .filter(Patient::isActive)
                .collect(Collectors.toList());
    }
}

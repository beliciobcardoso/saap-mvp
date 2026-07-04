package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListActivePatientsUseCase {

    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public PageResult<Patient> execute(int page, int size) {
        return patientRepository.findActive(page, size);
    }
}

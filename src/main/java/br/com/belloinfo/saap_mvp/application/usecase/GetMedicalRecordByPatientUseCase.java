package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.MedicalRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMedicalRecordByPatientUseCase {

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Transactional(readOnly = true)
    public MedicalRecord execute(UUID patientId, UUID currentProfessionalId) {
        if (!appointmentRepository.existsByPatientIdAndProfessionalId(patientId, currentProfessionalId)) {
            throw new AccessDeniedException("Profissional não tem acesso ao prontuário deste paciente");
        }

        return medicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente ainda não possui prontuário"));
    }
}

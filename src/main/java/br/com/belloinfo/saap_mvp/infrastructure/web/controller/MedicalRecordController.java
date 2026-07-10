package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.service.AuditService;
import br.com.belloinfo.saap_mvp.application.usecase.CreateMedicalRecordEntryUseCase;
import br.com.belloinfo.saap_mvp.application.usecase.GetMedicalRecordByPatientUseCase;
import br.com.belloinfo.saap_mvp.application.usecase.UpdateMedicalRecordEntryUseCase;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.CreateMedicalRecordEntryRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.MedicalRecordEntryResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.MedicalRecordResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.UpdateMedicalRecordEntryRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/medical-records")
@PreAuthorize("hasRole('PROFESSIONAL')")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final GetMedicalRecordByPatientUseCase getMedicalRecordByPatientUseCase;
    private final CreateMedicalRecordEntryUseCase createMedicalRecordEntryUseCase;
    private final UpdateMedicalRecordEntryUseCase updateMedicalRecordEntryUseCase;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final AuditService auditService;
    private final WebMapper mapper;

    private void logAudit(String action, UUID resourceId, HttpServletRequest httpRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            auditService.log(action, resourceId, "MEDICAL_RECORD", auth.getName(), httpRequest.getRemoteAddr());
        }
    }

    private Professional currentProfessional() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário logado não encontrado"));
        return professionalRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profissional não cadastrado para o usuário logado"));
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<MedicalRecordResponseDTO> getByPatient(@PathVariable UUID patientId, HttpServletRequest httpRequest) {
        MedicalRecord medicalRecord = getMedicalRecordByPatientUseCase.execute(patientId, currentProfessional().getId());
        logAudit("MEDICAL_RECORD_READ", medicalRecord.getId(), httpRequest);
        return ResponseEntity.ok(mapper.toResponse(medicalRecord));
    }

    @PostMapping("/entries")
    public ResponseEntity<MedicalRecordEntryResponseDTO> createEntry(
            @Valid @RequestBody CreateMedicalRecordEntryRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        MedicalRecordEntry entry = createMedicalRecordEntryUseCase.execute(
                request.appointmentId(),
                request.evolution(),
                currentProfessional().getId()
        );
        logAudit("MEDICAL_RECORD_ENTRY_CREATED", entry.getId(), httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(entry));
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<MedicalRecordEntryResponseDTO> updateEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody UpdateMedicalRecordEntryRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        MedicalRecordEntry entry = updateMedicalRecordEntryUseCase.execute(
                entryId,
                request.evolution(),
                currentProfessional().getId()
        );
        logAudit("MEDICAL_RECORD_ENTRY_UPDATED", entry.getId(), httpRequest);
        return ResponseEntity.ok(mapper.toResponse(entry));
    }
}

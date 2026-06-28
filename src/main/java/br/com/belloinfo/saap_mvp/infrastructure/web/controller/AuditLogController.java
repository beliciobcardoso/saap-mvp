package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.ListAuditLogsUseCase;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.AuditLogResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para exposição e consulta dos logs de auditoria do sistema.
 * Restrito exclusivamente a usuários com privilégios de administrador (ADMIN).
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final ListAuditLogsUseCase listAuditLogsUseCase;
    private final WebMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponseDTO>> list() {
        List<AuditLogResponseDTO> responseList = listAuditLogsUseCase.execute().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }
}

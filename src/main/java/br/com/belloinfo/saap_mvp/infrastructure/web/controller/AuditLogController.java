package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.ListAuditLogsUseCase;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.AuditLogResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.PageResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<PageResponseDTO<AuditLogResponseDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponseDTO.from(listAuditLogsUseCase.execute(page, size), mapper::toResponse));
    }
}

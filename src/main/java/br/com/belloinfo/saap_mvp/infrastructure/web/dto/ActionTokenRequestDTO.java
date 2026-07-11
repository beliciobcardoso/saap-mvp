package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ActionTokenRequestDTO(
    @NotBlank(message = "O token é obrigatório")
    String token
) {}

package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

public record LoginResponseDTO(
    String token,
    String tokenType,
    Long expiresIn
) {}

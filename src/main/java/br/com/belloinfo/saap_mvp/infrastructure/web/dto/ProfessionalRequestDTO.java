package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProfessionalRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String name,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "O telefone é obrigatório")
    String phone,

    @NotBlank(message = "O registro profissional é obrigatório")
    String registrationNumber,

    @NotNull(message = "O papel é obrigatório")
    ProfessionalRole role,

    UUID userId
) {}

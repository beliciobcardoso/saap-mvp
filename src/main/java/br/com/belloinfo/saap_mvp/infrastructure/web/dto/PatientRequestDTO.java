package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.infrastructure.web.validation.CPF;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record PatientRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String name,

    @NotBlank(message = "O CPF é obrigatório")
    @CPF
    String cpf,

    @Pattern(regexp = "\\d{15}", message = "O número do SUS deve conter exatamente 15 dígitos")
    String susNumber,

    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "O telefone é obrigatório")
    String phone,

    @NotNull(message = "A data de nascimento é obrigatória")
    @Past(message = "A data de nascimento deve estar no passado")
    LocalDate birthDate
) {}

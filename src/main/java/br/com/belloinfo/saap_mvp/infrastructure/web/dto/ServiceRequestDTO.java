package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ServiceRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String name,

    String description,

    @NotNull(message = "A duração é obrigatória")
    @Min(value = 1, message = "A duração mínima é de 1 minuto")
    Integer durationMinutes,

    @NotNull(message = "O preço é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    BigDecimal price
) {}

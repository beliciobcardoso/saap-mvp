package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookAppointmentRequestDTO(
    @NotNull(message = "O ID do paciente é obrigatório")
    UUID patientId,

    @NotNull(message = "O ID do profissional é obrigatório")
    UUID professionalId,

    @NotNull(message = "O ID do serviço é obrigatório")
    UUID serviceId,

    @NotNull(message = "A data e hora do agendamento são obrigatórias")
    @FutureOrPresent(message = "A data e hora do agendamento devem ser no presente ou futuro")
    LocalDateTime dateTime,

    @NotNull(message = "A forma de pagamento é obrigatória")
    PaymentMethod paymentMethod,

    PriorityLevel declaredPriority
) {}

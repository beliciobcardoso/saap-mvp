package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelAppointmentByTokenUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentActionTokenService tokenService;

    private CancelAppointmentByTokenUseCase useCase;

    private final UUID appointmentId = UUID.randomUUID();
    private final String token = "valid-token";

    @BeforeEach
    void setUp() {
        useCase = new CancelAppointmentByTokenUseCase(appointmentRepository, tokenService);
    }

    private Appointment pendingResponseAppointment() {
        return Appointment.builder()
                .id(appointmentId)
                .status(AppointmentStatus.PENDING_RESPONSE)
                .build();
    }

    @Test
    @DisplayName("cancela agendamento com token válido e status pending_response")
    void execute_validToken_cancelsAppointment() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(appointmentId, "cancel"));
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(pendingResponseAppointment()));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(token);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        verify(appointmentRepository).save(result);
    }

    @Test
    @DisplayName("lança exceção quando a ação do token não é cancel")
    void execute_wrongTokenAction_throwsException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(appointmentId, "confirm"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verify(appointmentRepository, never()).findById(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("propaga exceção quando o token é inválido ou expirado")
    void execute_invalidToken_propagatesException() {
        when(tokenService.validateToken(token))
                .thenThrow(new IllegalArgumentException("Token de ação inválido ou expirado"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("lança exceção quando agendamento não é encontrado")
    void execute_appointmentNotFound_throwsException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(appointmentId, "cancel"));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("lança exceção quando agendamento não está em pending_response")
    void execute_wrongStatus_throwsIllegalStateException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(appointmentId, "cancel"));
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(Appointment.builder().id(appointmentId).status(AppointmentStatus.CONFIRMED).build()));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        verify(appointmentRepository, never()).save(any());
    }
}

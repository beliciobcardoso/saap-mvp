package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptWaitlistOfferUseCaseTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentActionTokenService tokenService;

    @Mock
    private BookAppointmentUseCase bookAppointmentUseCase;

    private AcceptWaitlistOfferUseCase useCase;

    private final UUID entryId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID professionalId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();
    private final String token = "valid-token";
    private final LocalDateTime offeredTime = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        useCase = new AcceptWaitlistOfferUseCase(
                waitlistEntryRepository, appointmentRepository, tokenService, bookAppointmentUseCase);
    }

    private WaitlistEntry offeredEntry(LocalDateTime offerExpiresAt) {
        return WaitlistEntry.builder()
                .id(entryId)
                .patient(Patient.builder().id(patientId).name("Paciente Teste").build())
                .professional(Professional.builder().id(professionalId).build())
                .service(Service.builder().id(serviceId).build())
                .status(WaitlistStatus.OFFERED)
                .offeredAppointmentTime(offeredTime)
                .offerExpiresAt(offerExpiresAt)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("aceita oferta de vaga e confirma o novo agendamento")
    void execute_validOffer_confirmsAppointment() {
        WaitlistEntry entry = offeredEntry(LocalDateTime.now().plusMinutes(10));
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        Appointment bookedAppointment = Appointment.builder()
                .id(UUID.randomUUID())
                .status(AppointmentStatus.PENDING)
                .build();
        when(bookAppointmentUseCase.execute(eq(patientId), eq(professionalId), eq(serviceId), eq(offeredTime), any(), any()))
                .thenReturn(bookedAppointment);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = useCase.execute(token);

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        assertEquals(WaitlistStatus.ACCEPTED, entry.getStatus());
        assertFalse(entry.isActive());
        verify(waitlistEntryRepository).save(entry);
    }

    @Test
    @DisplayName("lança exceção quando a ação do token é inválida")
    void execute_wrongTokenAction_throwsException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "decline-waitlist"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verifyNoInteractions(waitlistEntryRepository, appointmentRepository, bookAppointmentUseCase);
    }

    @Test
    @DisplayName("lança exceção quando a entrada da fila não é encontrada")
    void execute_entryNotFound_throwsException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verify(waitlistEntryRepository, never()).save(any());
        verifyNoInteractions(appointmentRepository, bookAppointmentUseCase);
    }

    @Test
    @DisplayName("lança exceção quando a entrada não está ativa")
    void execute_inactiveEntry_throwsIllegalStateException() {
        WaitlistEntry entry = offeredEntry(LocalDateTime.now().plusMinutes(10));
        entry.setActive(false);
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        verify(waitlistEntryRepository, never()).save(any());
        verifyNoInteractions(appointmentRepository, bookAppointmentUseCase);
    }

    @Test
    @DisplayName("lança exceção quando a entrada não está com status offered")
    void execute_notOfferedStatus_throwsIllegalStateException() {
        WaitlistEntry entry = offeredEntry(LocalDateTime.now().plusMinutes(10));
        entry.setStatus(WaitlistStatus.WAITING);
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        verify(waitlistEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("expira a oferta e lança exceção quando o prazo já passou")
    void execute_expiredOffer_expiresAndThrowsException() {
        WaitlistEntry entry = offeredEntry(LocalDateTime.now().minusMinutes(5));
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        assertEquals(WaitlistStatus.EXPIRED, entry.getStatus());
        assertFalse(entry.isActive());
        verify(waitlistEntryRepository).save(entry);
        verifyNoInteractions(appointmentRepository, bookAppointmentUseCase);
    }

    @Test
    @DisplayName("retorna paciente para a fila quando há colisão de agendamento")
    void execute_bookingCollision_returnsEntryToWaitingList() {
        WaitlistEntry entry = offeredEntry(LocalDateTime.now().plusMinutes(10));
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(bookAppointmentUseCase.execute(eq(patientId), eq(professionalId), eq(serviceId), eq(offeredTime), any(), any()))
                .thenThrow(new IllegalStateException("Horário indisponível para este profissional"));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
        assertNull(entry.getOfferedAppointmentTime());
        assertNull(entry.getOfferExpiresAt());
        verify(waitlistEntryRepository).save(entry);
        verify(appointmentRepository, never()).save(any());
    }
}

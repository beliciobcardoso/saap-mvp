package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.repository.WaitlistEntryRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeclineWaitlistOfferUseCaseTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private AppointmentActionTokenService tokenService;

    @Mock
    private ProcessWaitlistSlotOfferUseCase processWaitlistSlotOfferUseCase;

    private DeclineWaitlistOfferUseCase useCase;

    private final UUID entryId = UUID.randomUUID();
    private final UUID professionalId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();
    private final String token = "valid-token";
    private final LocalDateTime offeredTime = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        useCase = new DeclineWaitlistOfferUseCase(waitlistEntryRepository, tokenService, processWaitlistSlotOfferUseCase);
    }

    private WaitlistEntry offeredEntry() {
        return WaitlistEntry.builder()
                .id(entryId)
                .patient(Patient.builder().id(UUID.randomUUID()).name("Paciente Teste").build())
                .professional(Professional.builder().id(professionalId).build())
                .service(Service.builder().id(serviceId).build())
                .status(WaitlistStatus.OFFERED)
                .offeredAppointmentTime(offeredTime)
                .offerExpiresAt(LocalDateTime.now().plusMinutes(10))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("recusa a oferta e cascateia para o próximo da fila")
    void execute_validOffer_declinesAndCascadesToNext() {
        WaitlistEntry entry = offeredEntry();
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "decline-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        useCase.execute(token);

        assertEquals(WaitlistStatus.DECLINED, entry.getStatus());
        assertFalse(entry.isActive());
        verify(waitlistEntryRepository).save(entry);
        verify(processWaitlistSlotOfferUseCase).execute(professionalId, serviceId, offeredTime);
    }

    @Test
    @DisplayName("lança exceção quando a ação do token é inválida")
    void execute_wrongTokenAction_throwsException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "accept-waitlist"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verifyNoInteractions(waitlistEntryRepository, processWaitlistSlotOfferUseCase);
    }

    @Test
    @DisplayName("lança exceção quando a entrada da fila não é encontrada")
    void execute_entryNotFound_throwsException() {
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "decline-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(token));

        verify(waitlistEntryRepository, never()).save(any());
        verifyNoInteractions(processWaitlistSlotOfferUseCase);
    }

    @Test
    @DisplayName("lança exceção quando a entrada não está ativa")
    void execute_inactiveEntry_throwsIllegalStateException() {
        WaitlistEntry entry = offeredEntry();
        entry.setActive(false);
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "decline-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        verify(waitlistEntryRepository, never()).save(any());
        verifyNoInteractions(processWaitlistSlotOfferUseCase);
    }

    @Test
    @DisplayName("lança exceção quando a entrada não está com status offered")
    void execute_notOfferedStatus_throwsIllegalStateException() {
        WaitlistEntry entry = offeredEntry();
        entry.setStatus(WaitlistStatus.WAITING);
        when(tokenService.validateToken(token))
                .thenReturn(new AppointmentActionTokenService.DecodedToken(entryId, "decline-waitlist"));
        when(waitlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> useCase.execute(token));

        verify(waitlistEntryRepository, never()).save(any());
        verifyNoInteractions(processWaitlistSlotOfferUseCase);
    }
}

package br.com.belloinfo.saap_mvp.domain.model;

import br.com.belloinfo.saap_mvp.domain.valueobject.WaitlistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntry {
    private UUID id;
    private Patient patient;
    private Professional professional;
    private Service service;
    private WaitlistStatus status;
    private LocalDateTime offeredAppointmentTime;
    private LocalDateTime offerExpiresAt;
    @Builder.Default
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void offer(LocalDateTime appointmentTime, long timeoutMinutes) {
        this.status = WaitlistStatus.OFFERED;
        this.offeredAppointmentTime = appointmentTime;
        this.offerExpiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
    }

    public void accept() {
        this.status = WaitlistStatus.ACCEPTED;
        this.active = false;
    }

    public void decline() {
        this.status = WaitlistStatus.DECLINED;
        this.active = false;
    }

    public void expire() {
        this.status = WaitlistStatus.EXPIRED;
        this.active = false;
    }
}

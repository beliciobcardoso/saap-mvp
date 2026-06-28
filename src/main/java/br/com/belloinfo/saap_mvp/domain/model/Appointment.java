package br.com.belloinfo.saap_mvp.domain.model;

import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private UUID id;
    private Patient patient;
    private Professional professional;
    private Service service;
    private LocalDateTime dateTime;
    private AppointmentStatus status;
    private PaymentMethod paymentMethod;
    
    // Prioridade e Fila Inteligente
    private PriorityLevel priorityLevel;
    private Long priorityScore;
    private LocalDateTime priorityDeclaredAt;
    private UUID priorityVerifiedBy;
    private String priorityNotes;
    
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void transitionTo(AppointmentStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }
        boolean valid = false;
        switch (this.status) {
            case PENDING:
                valid = (newStatus == AppointmentStatus.CONFIRMED || newStatus == AppointmentStatus.CANCELLED);
                break;
            case CONFIRMED:
                valid = (newStatus == AppointmentStatus.ARRIVED || newStatus == AppointmentStatus.CANCELLED || newStatus == AppointmentStatus.NO_SHOW);
                break;
            case ARRIVED:
                valid = (newStatus == AppointmentStatus.CALLING || newStatus == AppointmentStatus.CANCELLED);
                break;
            case CALLING:
                valid = (newStatus == AppointmentStatus.IN_PROGRESS);
                break;
            case IN_PROGRESS:
                valid = (newStatus == AppointmentStatus.COMPLETED);
                break;
            case COMPLETED:
            case CANCELLED:
            case NO_SHOW:
                valid = false;
                break;
        }
        if (!valid) {
            throw new IllegalStateException("Transição de estado inválida de " + this.status + " para " + newStatus);
        }
        this.status = newStatus;
    }

    public void checkIn(PriorityLevel verifiedLevel, UUID verifiedBy, String notes, long checkInTimestamp) {
        transitionTo(AppointmentStatus.ARRIVED);
        this.priorityLevel = verifiedLevel;
        this.priorityVerifiedBy = verifiedBy;
        this.priorityNotes = notes;
        this.priorityScore = (verifiedLevel.getValue() * 1_000_000_000_000L) + checkInTimestamp;
    }
}

package br.com.belloinfo.saap_mvp.domain.model;

import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentDomainTest {

    @Test
    void shouldCreateAppointmentWithDefaultValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now().plusDays(1);
        Appointment appointment = Appointment.builder()
                .id(id)
                .dateTime(time)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(PaymentMethod.PIX)
                .priorityLevel(PriorityLevel.P5)
                .build();

        assertEquals(id, appointment.getId());
        assertEquals(time, appointment.getDateTime());
        assertEquals(AppointmentStatus.PENDING, appointment.getStatus());
        assertEquals(PaymentMethod.PIX, appointment.getPaymentMethod());
        assertEquals(PriorityLevel.P5, appointment.getPriorityLevel());
        assertNull(appointment.getPriorityScore());
    }

    @Test
    void shouldAllowValidStateTransitions() {
        Appointment appointment = Appointment.builder()
                .status(AppointmentStatus.PENDING)
                .build();

        // PENDING -> CONFIRMED
        appointment.transitionTo(AppointmentStatus.CONFIRMED);
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());

        // CONFIRMED -> ARRIVED
        appointment.transitionTo(AppointmentStatus.ARRIVED);
        assertEquals(AppointmentStatus.ARRIVED, appointment.getStatus());

        // ARRIVED -> CALLING
        appointment.transitionTo(AppointmentStatus.CALLING);
        assertEquals(AppointmentStatus.CALLING, appointment.getStatus());

        // CALLING -> IN_PROGRESS
        appointment.transitionTo(AppointmentStatus.IN_PROGRESS);
        assertEquals(AppointmentStatus.IN_PROGRESS, appointment.getStatus());

        // IN_PROGRESS -> COMPLETED
        appointment.transitionTo(AppointmentStatus.COMPLETED);
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    void shouldAllowCancellationFromPendingAndConfirmedAndArrived() {
        // From PENDING
        Appointment app1 = Appointment.builder().status(AppointmentStatus.PENDING).build();
        app1.transitionTo(AppointmentStatus.CANCELLED);
        assertEquals(AppointmentStatus.CANCELLED, app1.getStatus());

        // From CONFIRMED
        Appointment app2 = Appointment.builder().status(AppointmentStatus.CONFIRMED).build();
        app2.transitionTo(AppointmentStatus.CANCELLED);
        assertEquals(AppointmentStatus.CANCELLED, app2.getStatus());

        // From ARRIVED
        Appointment app3 = Appointment.builder().status(AppointmentStatus.ARRIVED).build();
        app3.transitionTo(AppointmentStatus.CANCELLED);
        assertEquals(AppointmentStatus.CANCELLED, app3.getStatus());
    }

    @Test
    void shouldThrowExceptionForInvalidTransitions() {
        Appointment appointment = Appointment.builder()
                .status(AppointmentStatus.PENDING)
                .build();

        // PENDING -> CALLING is invalid
        assertThrows(IllegalStateException.class, () -> appointment.transitionTo(AppointmentStatus.CALLING));

        // Move to CONFIRMED
        appointment.transitionTo(AppointmentStatus.CONFIRMED);

        // CONFIRMED -> IN_PROGRESS is invalid
        assertThrows(IllegalStateException.class, () -> appointment.transitionTo(AppointmentStatus.IN_PROGRESS));
    }

    @Test
    void shouldCalculateCorrectPriorityScoreOnCheckIn() {
        Appointment appointment = Appointment.builder()
                .status(AppointmentStatus.CONFIRMED)
                .build();

        UUID receptionistId = UUID.randomUUID();
        long timestamp = 1782500000000L; // Millisecond timestamp

        appointment.checkIn(PriorityLevel.P1, receptionistId, "Laudo de TEA apresentado", timestamp);

        assertEquals(AppointmentStatus.ARRIVED, appointment.getStatus());
        assertEquals(PriorityLevel.P1, appointment.getPriorityLevel());
        assertEquals(receptionistId, appointment.getPriorityVerifiedBy());
        assertEquals("Laudo de TEA apresentado", appointment.getPriorityNotes());
        
        // P1 value is 1. Score = 1 * 10^12 + timestamp
        long expectedScore = 1_000_000_000_000L + timestamp;
        assertEquals(expectedScore, appointment.getPriorityScore());
    }

    @Test
    void shouldEnsureEarlierTimestampHasLowerScoreForSamePriorityLevel() {
        Appointment app1 = Appointment.builder().status(AppointmentStatus.CONFIRMED).build();
        Appointment app2 = Appointment.builder().status(AppointmentStatus.CONFIRMED).build();

        UUID receptionistId = UUID.randomUUID();
        long earlyTimestamp = 1000L;
        long lateTimestamp = 2000L;

        app1.checkIn(PriorityLevel.P2, receptionistId, "Idoso 65 anos", earlyTimestamp);
        app2.checkIn(PriorityLevel.P2, receptionistId, "Idoso 70 anos", lateTimestamp);

        // Score of app1 (early check-in) must be smaller than app2 (late check-in)
        assertTrue(app1.getPriorityScore() < app2.getPriorityScore());
    }

    @Test
    void shouldEnsureP1CheckInIsPrioritizedOverP2RegardlessOfTimestamp() {
        Appointment appP1 = Appointment.builder().status(AppointmentStatus.CONFIRMED).build();
        Appointment appP2 = Appointment.builder().status(AppointmentStatus.CONFIRMED).build();

        UUID receptionist = UUID.randomUUID();
        
        // P1 checks in late, P2 checked in early
        appP1.checkIn(PriorityLevel.P1, receptionist, "Deficiente", 9999999L);
        appP2.checkIn(PriorityLevel.P2, receptionist, "Doador", 1000L);

        // AppP1 score must be lower than AppP2 score
        assertTrue(appP1.getPriorityScore() < appP2.getPriorityScore());
    }
}

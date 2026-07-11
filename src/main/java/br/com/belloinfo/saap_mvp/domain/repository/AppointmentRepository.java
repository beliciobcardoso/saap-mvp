package br.com.belloinfo.saap_mvp.domain.repository;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    boolean existsByProfessionalIdAndDateTimeAndStatusNotIn(UUID professionalId, LocalDateTime dateTime, List<AppointmentStatus> statuses);
    PageResult<Appointment> findByFilters(UUID professionalId, UUID patientId, LocalDateTime startDateTime, LocalDateTime endDateTime, int page, int size);

    /**
     * Busca o próximo agendamento na fila com lock pessimista para evitar race conditions.
     * Trava o agendamento encontrado para impedir que múltiplas requisições simultâneas peguem o mesmo paciente.
     */
    Optional<Appointment> findNextInQueueWithLock(UUID professionalId, LocalDateTime start, LocalDateTime end);

    /**
     * Busca agendamentos elegíveis para receber notificação de follow-up:
     * status = PENDING, followUpSentAt IS NULL e data dentro da janela [windowStart, windowEnd].
     */
    List<Appointment> findEligibleForFollowUp(LocalDateTime windowStart, LocalDateTime windowEnd);

    /**
     * Busca agendamentos em PENDING_RESPONSE cujo horário é <= deadline informado
     * (ou seja, a consulta está próxima e o paciente ainda não respondeu).
     */
    List<Appointment> findPendingResponsePastDeadline(LocalDateTime deadline);

    /**
     * Verifica se existe agendamento com paciente e profissional específicos.
     */
    boolean existsByPatientIdAndProfessionalId(UUID patientId, UUID professionalId);
}

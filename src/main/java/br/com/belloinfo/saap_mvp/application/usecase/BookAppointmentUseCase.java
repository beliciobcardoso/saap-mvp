package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.exception.ScheduleConflictException;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.PatientRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ServiceRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.domain.valueobject.PaymentMethod;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;

    @org.springframework.transaction.annotation.Transactional
    public Appointment execute(UUID patientId, UUID professionalId, UUID serviceId, LocalDateTime dateTime, PaymentMethod paymentMethod, PriorityLevel declaredPriority) {
        
        // Lock pessimista no profissional para evitar condições de corrida pelo mesmo horário
        Professional professional = professionalRepository.findByIdWithLock(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado"));

        // Valida conflito de horário
        boolean hasConflict = appointmentRepository.existsByProfessionalIdAndDateTimeAndStatusNotIn(
                professionalId,
                dateTime,
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        );

        if (hasConflict) {
            throw new ScheduleConflictException("Horário indisponível para este profissional");
        }

        PriorityLevel priority = declaredPriority != null ? declaredPriority : PriorityLevel.P5;

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .professional(professional)
                .service(service)
                .dateTime(dateTime)
                .status(AppointmentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .priorityLevel(priority)
                .priorityDeclaredAt(priority != PriorityLevel.P5 ? LocalDateTime.now() : null)
                .build();

        return appointmentRepository.save(appointment);
    }
}

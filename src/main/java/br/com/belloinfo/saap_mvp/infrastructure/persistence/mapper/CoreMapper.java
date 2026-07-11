package br.com.belloinfo.saap_mvp.infrastructure.persistence.mapper;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecord;
import br.com.belloinfo.saap_mvp.domain.model.MedicalRecordEntry;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AppointmentEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.MedicalRecordEntryEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.WaitlistEntryEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.PatientEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.ProfessionalEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.ServiceEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.UserEntity;
import br.com.belloinfo.saap_mvp.infrastructure.persistence.entity.AuditLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CoreMapper {

    CoreMapper INSTANCE = Mappers.getMapper(CoreMapper.class);

    // User Mapping
    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);

    // Patient Mapping
    Patient toDomain(PatientEntity entity);
    PatientEntity toEntity(Patient domain);

    // Service Mapping
    Service toDomain(ServiceEntity entity);
    ServiceEntity toEntity(Service domain);

    // Professional Mapping
    @Mapping(target = "userId", source = "user.id")
    Professional toDomain(ProfessionalEntity entity);

    @Mapping(target = "user", expression = "java(mapUserIdToUserEntity(domain.getUserId()))")
    ProfessionalEntity toEntity(Professional domain);

    // Appointment Mapping
    Appointment toDomain(AppointmentEntity entity);
    AppointmentEntity toEntity(Appointment domain);

    // WaitlistEntry Mapping
    WaitlistEntry toDomain(WaitlistEntryEntity entity);
    WaitlistEntryEntity toEntity(WaitlistEntry domain);

    // AuditLog Mapping
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "appointmentId", source = "appointment.id")
    AuditLog toDomain(AuditLogEntity entity);

    @Mapping(target = "user", expression = "java(mapUserIdToUserEntity(domain.getUserId()))")
    @Mapping(target = "appointment", expression = "java(mapAppointmentIdToAppointmentEntity(domain.getAppointmentId()))")
    AuditLogEntity toEntity(AuditLog domain);

    // MedicalRecord Mapping
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "entries", ignore = true)
    MedicalRecord toDomain(MedicalRecordEntity entity);

    @Mapping(target = "patient", expression = "java(mapPatientIdToPatientEntity(domain.getPatientId()))")
    @Mapping(target = "entries", ignore = true)
    MedicalRecordEntity toEntity(MedicalRecord domain);

    // MedicalRecordEntry Mapping
    @Mapping(target = "medicalRecordId", source = "medicalRecord.id")
    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "professionalId", source = "professional.id")
    MedicalRecordEntry toDomain(MedicalRecordEntryEntity entity);

    @Mapping(target = "medicalRecord", expression = "java(mapMedicalRecordIdToMedicalRecordEntity(domain.getMedicalRecordId()))")
    @Mapping(target = "appointment", expression = "java(mapAppointmentIdToAppointmentEntity(domain.getAppointmentId()))")
    @Mapping(target = "professional", expression = "java(mapProfessionalIdToProfessionalEntity(domain.getProfessionalId()))")
    MedicalRecordEntryEntity toEntity(MedicalRecordEntry domain);

    default MedicalRecordEntity mapMedicalRecordIdToMedicalRecordEntity(UUID medicalRecordId) {
        if (medicalRecordId == null) {
            return null;
        }
        MedicalRecordEntity medicalRecord = new MedicalRecordEntity();
        medicalRecord.setId(medicalRecordId);
        return medicalRecord;
    }

    default PatientEntity mapPatientIdToPatientEntity(UUID patientId) {
        if (patientId == null) {
            return null;
        }
        PatientEntity patient = new PatientEntity();
        patient.setId(patientId);
        return patient;
    }

    default ProfessionalEntity mapProfessionalIdToProfessionalEntity(UUID professionalId) {
        if (professionalId == null) {
            return null;
        }
        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setId(professionalId);
        return professional;
    }

    default UserEntity mapUserIdToUserEntity(UUID userId) {
        if (userId == null) {
            return null;
        }
        UserEntity user = new UserEntity();
        user.setId(userId);
        return user;
    }

    default AppointmentEntity mapAppointmentIdToAppointmentEntity(UUID appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(appointmentId);
        return appointment;
    }
}

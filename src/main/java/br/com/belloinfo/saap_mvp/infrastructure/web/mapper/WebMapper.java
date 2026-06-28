package br.com.belloinfo.saap_mvp.infrastructure.web.mapper;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface WebMapper {

    WebMapper INSTANCE = Mappers.getMapper(WebMapper.class);

    // User
    User toDomain(UserRequestDTO request);
    UserResponseDTO toResponse(User domain);

    // Patient
    Patient toDomain(PatientRequestDTO request);
    PatientResponseDTO toResponse(Patient domain);

    // Professional
    Professional toDomain(ProfessionalRequestDTO request);
    ProfessionalResponseDTO toResponse(Professional domain);

    // Service
    Service toDomain(ServiceRequestDTO request);
    ServiceResponseDTO toResponse(Service domain);

    // Appointment
    AppointmentResponseDTO toResponse(Appointment domain);
}

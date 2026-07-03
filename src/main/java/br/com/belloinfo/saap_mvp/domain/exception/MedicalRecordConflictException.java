package br.com.belloinfo.saap_mvp.domain.exception;

/**
 * Conflito de regra de negócio do prontuário: janela de edição fechada
 * (agendamento fora de IN_PROGRESS), entrada duplicada para o mesmo
 * agendamento ou finalização de consulta sem evolução preenchida.
 * Mapeada para HTTP 409 (Conflict).
 */
public class MedicalRecordConflictException extends RuntimeException {
    public MedicalRecordConflictException(String message) {
        super(message);
    }
}

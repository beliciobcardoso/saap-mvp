package br.com.belloinfo.saap_mvp.domain.exception;

/**
 * Recurso de domínio não encontrado (paciente, profissional, serviço, usuário, etc).
 * Mapeada para HTTP 404 (Not Found).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

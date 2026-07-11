package br.com.belloinfo.saap_mvp.domain.exception;

public class ScheduleConflictException extends RuntimeException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}

package br.com.belloinfo.saap_mvp.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CPF {
    String message() default "O CPF informado é inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

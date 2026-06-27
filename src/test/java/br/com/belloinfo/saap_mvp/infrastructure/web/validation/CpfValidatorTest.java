package br.com.belloinfo.saap_mvp.infrastructure.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfValidatorTest {

    private CpfValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new CpfValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
    }

    @Test
    void shouldReturnTrueWhenCpfIsNull() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void shouldReturnTrueWhenCpfIsEmpty() {
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
    }

    @Test
    void shouldReturnFalseWhenCpfHasInvalidLength() {
        assertFalse(validator.isValid("123", context));
        assertFalse(validator.isValid("123456789012", context));
    }

    @Test
    void shouldReturnFalseWhenCpfHasRepeatedDigits() {
        assertFalse(validator.isValid("11111111111", context));
        assertFalse(validator.isValid("00000000000", context));
        assertFalse(validator.isValid("99999999999", context));
    }

    @Test
    void shouldReturnTrueWhenCpfIsValidPlain() {
        assertTrue(validator.isValid("52998224725", context));
    }

    @Test
    void shouldReturnTrueWhenCpfIsValidFormatted() {
        assertTrue(validator.isValid("529.982.247-25", context));
    }

    @Test
    void shouldReturnFalseWhenCpfHasInvalidVerificationDigits() {
        assertFalse(validator.isValid("52998224726", context));
        assertFalse(validator.isValid("52998224715", context));
    }
}

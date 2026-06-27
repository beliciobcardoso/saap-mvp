package br.com.belloinfo.saap_mvp.infrastructure.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotNull/@NotBlank handle empty/null cases if needed
        }

        // Remove non-digits
        String cleanValue = value.replaceAll("\\D", "");

        if (cleanValue.length() != 11) {
            return false;
        }

        // Reject known invalid CPFs
        if (cleanValue.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int d1 = calculateDigit(cleanValue.substring(0, 9), 10);
            int d2 = calculateDigit(cleanValue.substring(0, 9) + d1, 11);
            return cleanValue.equals(cleanValue.substring(0, 9) + d1 + d2);
        } catch (Exception e) {
            return false;
        }
    }

    private int calculateDigit(String base, int weight) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weight--;
        }
        int remainder = (sum * 10) % 11;
        return (remainder == 10 || remainder == 11) ? 0 : remainder;
    }
}

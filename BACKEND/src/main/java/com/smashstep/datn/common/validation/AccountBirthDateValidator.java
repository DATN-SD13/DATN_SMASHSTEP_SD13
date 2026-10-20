package com.smashstep.datn.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class AccountBirthDateValidator implements ConstraintValidator<AccountBirthDate, LocalDate> {
    private int minAge;
    @Override public void initialize(AccountBirthDate annotation) { minAge = annotation.minAge(); }
    @Override public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value.getYear() >= 1900 && !value.isAfter(LocalDate.now().minusYears(minAge))) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Ngày sinh phải từ năm 1900 và đủ " + minAge + " tuổi trở lên").addConstraintViolation();
        return false;
    }
}

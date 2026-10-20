package com.smashstep.datn.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AccountTextValidator implements ConstraintValidator<AccountText, String> {
    private AccountRules.Kind kind;
    @Override public void initialize(AccountText annotation) { kind = annotation.value(); }
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        String error = AccountRules.error(kind, value);
        if (error == null) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }
}

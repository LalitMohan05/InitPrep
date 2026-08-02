package com.initprep.user.validation.annotation;

import com.initprep.user.validation.enums.Platform;
import com.initprep.user.validation.validator.ProfileUrlValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ProfileUrlValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProfileUrl {

    Platform platform();

    String message() default "Invalid profile URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

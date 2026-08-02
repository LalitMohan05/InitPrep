package com.initprep.user.validation.validator;

import com.initprep.user.validation.annotation.ProfileUrl;
import com.initprep.user.validation.enums.Platform;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

public class ProfileUrlValidator
    implements ConstraintValidator<ProfileUrl, String> {

    private Platform platform;

    private static final Map<Platform, String> REGEX = Map.of(
        Platform.GITHUB,
        "^https://(www\\.)?github\\.com/[A-Za-z0-9-]+/?$",

        Platform.LINKEDIN,
        "^https://(www\\.)?linkedin\\.com/in/[A-Za-z0-9-_%]+/?$",

        Platform.LEETCODE,
        "^https://(www\\.)?leetcode\\.com/u?/?.+$",

        Platform.CODEFORCES,
        "^https://(www\\.)?codeforces\\.com/profile/.+$",

        Platform.CODECHEF,
        "^https://(www\\.)?codechef\\.com/users/.+$"
    );

    @Override
    public void initialize(ProfileUrl constraintAnnotation) {
        this.platform = constraintAnnotation.platform();
    }

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return value.matches(REGEX.get(platform));
    }
}

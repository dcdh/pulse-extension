package com.damdamdeo.pulse.extension.core.connecteduser;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record Username(String username) {

    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Username {
        Objects.requireNonNull(username);
        Validate.validState(matchEmailPattern(username));
    }

    public static boolean matchEmailPattern(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}

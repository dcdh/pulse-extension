package com.damdamdeo.pulse.extension.core.connecteduser;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record UsernameEncoded(String encoded) {

    public static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9\\+\\/]+=*$");

    public UsernameEncoded {
        Objects.requireNonNull(encoded);
        Validate.validState(matchBase64Pattern(encoded));
    }

    public static boolean matchBase64Pattern(String email) {
        return BASE64_PATTERN.matcher(email).matches();
    }
}

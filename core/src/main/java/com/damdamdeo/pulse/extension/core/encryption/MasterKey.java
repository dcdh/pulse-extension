package com.damdamdeo.pulse.extension.core.encryption;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record MasterKey(String key) {

    public MasterKey {
        Objects.requireNonNull(key);
        Validate.matchesPattern(key, "[0-9a-zA-Z]{32}");
    }

    public Passphrase toPassphrase() {
        return Passphrase.ofValid(key.toCharArray());
    }
}

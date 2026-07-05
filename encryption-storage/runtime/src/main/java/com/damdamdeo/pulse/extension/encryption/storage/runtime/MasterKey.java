package com.damdamdeo.pulse.extension.encryption.storage.runtime;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record MasterKey(String key) {

    public MasterKey {
        Objects.requireNonNull(key);
        Validate.matchesPattern(key, "[0-9a-zA-Z]{32}");
    }
}

package com.damdamdeo.pulse.extension.core.hashing;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record Hash<T extends Identifiable>(String value) {

    public Hash {
        Objects.requireNonNull(value);
        Validate.validState(value.matches("^[a-f0-9]{64}$"), "invalid hash");
    }
}

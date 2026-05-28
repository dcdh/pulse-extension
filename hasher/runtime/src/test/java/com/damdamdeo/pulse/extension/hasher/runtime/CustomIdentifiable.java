package com.damdamdeo.pulse.extension.hasher.runtime;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;

import java.util.Objects;

public record CustomIdentifiable(String id, Hash<CustomIdentifiable> expected) implements Identifiable {

    public static final CustomIdentifiable GIVEN = new CustomIdentifiable("test",
            new Hash<>("36f028580bb02cc8272a9a020f4200e346e276ae664e45ee80745574e2f5ab80"));

    public CustomIdentifiable {
        Objects.requireNonNull(id);
        Objects.requireNonNull(expected);
    }
}
